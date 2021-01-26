package functionsalgo.binanceperpetual.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import functionsalgo.binanceperpetual.dataprovider.BPHistoricFundingRates;
import functionsalgo.binanceperpetual.dataprovider.BPHistoricKlines;
import functionsalgo.datapoints.FundingRate;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;

public class BPSimExchange implements BPExchange {
    
    @SuppressWarnings("unused")
    private static final boolean IS_HEDGE_MODE = true;
    
    private static final double OPEN_LOSS_SIM_MULT = 1.06; // will probably never be this high
    
    public static final double TAKER_OPEN_CLOSE_FEE = 0.0004;
    public static final double MAKER_OPEN_CLOSE_FEE = 0.0002;
    
    private BPSimAccount accInfo;
    
    private long fundingIntervalMillis;
    private long nextFundingTime;
    private short defaultLeverage;
    private long updateIntervalMillis;
    
    private BPHistoricKlines bpHistoricKlines;
    private BPHistoricFundingRates bpHistoricFundingRates;
    private BPSlippageModel slippageModel;
    
    private List<BatchedOrder> batchedMarketOrders;
    
    private class BatchedOrder {
        
        String orderId;
        String symbol;
        boolean isLong;
        double quantity;
        boolean isOpen;
        
        public BatchedOrder(String orderId, String symbol, boolean isLong, double quantity, boolean isOpen) {
            
            this.orderId = orderId;
            this.symbol = symbol;
            this.isLong = isLong;
            this.quantity = quantity;
            this.isOpen = isOpen;
        }
    }
    
    public BPSimExchange(double walletBalance, short defaultLeverage, Interval updateInterval) {
        
        accInfo = new BPSimAccount(walletBalance);
        this.defaultLeverage = defaultLeverage;
        updateIntervalMillis = updateInterval.toMilliseconds();
        bpHistoricKlines = BPHistoricKlines.loadKlines(updateInterval);
        bpHistoricFundingRates = BPHistoricFundingRates.loadFundingRates();
        fundingIntervalMillis = bpHistoricFundingRates.getFundingIntervalMillis();
        slippageModel = BPSlippageModel.LoadSlippageModel(BPSlippageModel.MODEL_FILE);
        batchedMarketOrders = new ArrayList<>();
    }
    
    @Override
    public BPAccount getAccountInfo(long timestamp) {
        
        updateAccountInfo(timestamp);
        
        return accInfo;
    }
    
    void updateAccountInfo(long timestamp) {
        
        // TODO random position adl close, simulate not being able to trade for a period of time
        if (timestamp <= accInfo.lastUpdatedTime) {
            throw new IllegalArgumentException("timestamp must be larger than the previous");
        }
        
        if (accInfo.lastUpdatedTime != 0) {
            
            for (long time = accInfo.lastUpdatedTime + updateIntervalMillis; time <= timestamp; time += updateIntervalMillis) {
                
                calculateMarginBalanceAndUpdatePositionData(time);
                checkMaintenanceMargin(time);
                
                if (time >= nextFundingTime) {
                    nextFundingTime += fundingIntervalMillis;
                }
            }
            accInfo.nextFundingTime = nextFundingTime;
            accInfo.lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
        } else {
            nextFundingTime = ((timestamp / fundingIntervalMillis) * fundingIntervalMillis) + fundingIntervalMillis;
            accInfo.nextFundingTime = nextFundingTime;
            calculateMarginBalanceAndUpdatePositionData(timestamp);
            checkMaintenanceMargin(timestamp);
            
            accInfo.lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
        }
        
    }
    
    void calculateMarginBalanceAndUpdatePositionData(long timestamp) {
        
        accInfo.marginBalance = accInfo.walletBalance;
        accInfo.worstCurrentMarginBalance = accInfo.walletBalance;
        
        for (Map.Entry<String, BPSimAccount.PositionData> entry : accInfo.positions.entrySet()) {
            // should use mark price for more accuracy
            Kline kline = bpHistoricKlines.getKlines(entry.getValue().symbol, timestamp, timestamp).get(0);
            if (entry.getValue().isLong) {
                double currPrice = kline.getOpen();
                entry.getValue().currPrice = currPrice;
                double pnl = (entry.getValue().currPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
                accInfo.marginBalance += pnl;
                double worstPrice = kline.getLow();
                double worstPnL = (worstPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
                accInfo.worstCurrentMarginBalance += worstPnL;
                int leverage = accInfo.leverages.containsKey(entry.getValue().symbol)
                        ? accInfo.leverages.get(entry.getValue().symbol)
                        : defaultLeverage;
                entry.getValue().margin = (entry.getValue().currPrice * entry.getValue().quantity) / leverage;
            } else {
                double currPrice = kline.getOpen();
                entry.getValue().currPrice = currPrice;
                double pnl = (entry.getValue().avgOpenPrice - entry.getValue().currPrice) * entry.getValue().quantity;
                accInfo.marginBalance += pnl;
                double worstPrice = kline.getHigh();
                double worstPnL = (entry.getValue().avgOpenPrice - worstPrice) * entry.getValue().quantity;
                accInfo.worstCurrentMarginBalance += worstPnL;
                int leverage = accInfo.leverages.containsKey(entry.getValue().symbol)
                        ? accInfo.leverages.get(entry.getValue().symbol)
                        : defaultLeverage;
                entry.getValue().margin = (entry.getValue().currPrice * entry.getValue().quantity) / leverage;
            }
        }
        
        if (timestamp >= nextFundingTime) {
            for (Map.Entry<String, BPSimAccount.PositionData> entry : accInfo.positions.entrySet()) {
                FundingRate frate = bpHistoricFundingRates.getFundingRates(entry.getValue().symbol, timestamp, timestamp).get(0);
                if (entry.getValue().isLong) {
                    double fundingRate = frate.getFundingRate();
                    accInfo.fundingRates.put(entry.getValue().symbol, fundingRate);
                    double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                    accInfo.marginBalance -= funding;
                    accInfo.walletBalance -= funding;
                    accInfo.worstCurrentMarginBalance -= funding;
                } else {
                    double fundingRate = frate.getFundingRate();
                    accInfo.fundingRates.put(entry.getValue().symbol, fundingRate);
                    double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                    accInfo.marginBalance += funding;
                    accInfo.walletBalance += funding;
                    accInfo.worstCurrentMarginBalance += funding;
                }
            }
        }
    }
    
    void checkMaintenanceMargin(long time) {
        
        // assuming crossed margin type
        double highestInitialMargin = 0;
        
        for (Map.Entry<String, BPSimAccount.PositionData> entry : accInfo.positions.entrySet()) {
            
            highestInitialMargin = Math.max(highestInitialMargin, entry.getValue().margin);
        }
        // the maintenance margin is *always less* than 50% of the initial margin
        if (accInfo.worstCurrentMarginBalance <= highestInitialMargin / 2) {
            // TODO throw some exception
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("!!! Probably going to get liquidated at: " + time);
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            accInfo.marginBalance = 0;
            accInfo.walletBalance = 0;
            accInfo.positions = new HashMap<>();
        }
    }
    
    private boolean marketOpen(String symbol, boolean isLong, double symbolQty) {
        
        String positionId = getPositionId(symbol, isLong);
        Kline kline = bpHistoricKlines.getKlines(symbol, accInfo.lastUpdatedTime, accInfo.lastUpdatedTime).get(0);
        double openPrice = kline.getOpen();
        double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol) : defaultLeverage;
        double initialMargin = (openPrice * symbolQty) / leverage;
        if (isLong) {
            openPrice *= slippageModel.getSlippage(openPrice * symbolQty, symbol);
        } else {
            openPrice *= 1 - (slippageModel.getSlippage(openPrice * symbolQty, symbol) - 1);
        }
        double notionalValue = openPrice * symbolQty;
        
        double marginUsed = 0;
        for (Map.Entry<String, BPSimAccount.PositionData> entry : accInfo.positions.entrySet()) {
            marginUsed += entry.getValue().margin;
        }
        if (marginUsed + (initialMargin * OPEN_LOSS_SIM_MULT) >= accInfo.marginBalance) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("!!! Not enough margin to open position at: " + accInfo.lastUpdatedTime);
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            // TODO log no margin to open position
            return false;
        }
        
        double openFee = notionalValue * TAKER_OPEN_CLOSE_FEE;
        accInfo.marginBalance -= openFee;
        accInfo.walletBalance -= openFee;
        
        if (accInfo.positions.containsKey(positionId)) {
            accInfo.positions.get(positionId).margin += initialMargin;
            double newQty = accInfo.positions.get(positionId).quantity + symbolQty;
            double percOldQty = accInfo.positions.get(positionId).quantity / newQty;
            double prevAvgOpenPrice = accInfo.positions.get(positionId).avgOpenPrice;
            double percNewQty = symbolQty / newQty;
            accInfo.positions.get(positionId).avgOpenPrice = (prevAvgOpenPrice * percOldQty) + (openPrice * percNewQty);
            accInfo.positions.get(positionId).quantity = newQty;
        } else {
            accInfo.positions.put(positionId, accInfo.new PositionData(symbol, isLong, openPrice, symbolQty, initialMargin));
        }
        // TODO log successful open
        return true;
    }
    
    private boolean marketClose(String symbol, boolean isLong, double qtyToClose) {
        
        String positionId = getPositionId(symbol, isLong);
        
        if (accInfo.positions.containsKey(positionId)) {
            
            if (accInfo.positions.get(positionId).quantity > qtyToClose) {
                
                double nValSlipp = qtyToClose * accInfo.positions.get(positionId).currPrice;
                double closePrice;
                double pnl;
                if (accInfo.positions.get(positionId).isLong) {
                    closePrice = accInfo.positions.get(positionId).currPrice
                            * (1 - (slippageModel.getSlippage(nValSlipp, symbol) - 1));
                    pnl = (closePrice - accInfo.positions.get(positionId).avgOpenPrice) * qtyToClose;
                } else {
                    closePrice = accInfo.positions.get(positionId).currPrice * slippageModel.getSlippage(nValSlipp, symbol);
                    pnl = (accInfo.positions.get(positionId).avgOpenPrice - closePrice) * qtyToClose;
                }
                double notionalValue = closePrice * qtyToClose;
                double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;
                
                accInfo.walletBalance -= closeFee;
                accInfo.marginBalance -= closeFee;
                accInfo.walletBalance += pnl;
                
                double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol) : defaultLeverage;
                double margin = notionalValue / leverage;
                accInfo.positions.get(positionId).margin -= margin;
                accInfo.positions.get(positionId).quantity -= qtyToClose;
                
                // TODO log successful close
            } else {
                double quantity = accInfo.positions.get(positionId).quantity;
                double nValSlipp = quantity * accInfo.positions.get(positionId).currPrice;
                double closePrice;
                double pnl;
                if (accInfo.positions.get(positionId).isLong) {
                    closePrice = accInfo.positions.get(positionId).currPrice
                            * (1 - (slippageModel.getSlippage(nValSlipp, symbol) - 1));
                    pnl = (closePrice - accInfo.positions.get(positionId).avgOpenPrice) * quantity;
                } else {
                    closePrice = accInfo.positions.get(positionId).currPrice * slippageModel.getSlippage(nValSlipp, symbol);
                    pnl = (accInfo.positions.get(positionId).avgOpenPrice - closePrice) * quantity;
                }
                double notionalValue = closePrice * quantity;
                double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;
                
                accInfo.walletBalance -= closeFee;
                accInfo.marginBalance -= closeFee;
                accInfo.walletBalance += pnl;
                
                accInfo.positions.remove(positionId);
                
                // TODO log successful close
            }
        } else {
            // TODO log no position with positionId
            return false;
        }
        
        return true;
    }
    
    public static String getPositionId(String symbol, boolean isLong) {
        
        return symbol + "_" + isLong;
    }
    
    @Override
    public void batchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty) {
        
        accInfo.ordersWithErrors.remove(orderId);
        batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, symbolQty, true));
    }
    
    @Override
    public void batchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose) {
        
        accInfo.ordersWithErrors.remove(orderId);
        batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, qtyToClose, false));
    }
    
    @Override
    public BPAccount executeBatchedOrders() {
        
        ArrayList<BatchedOrder> tempBatch = new ArrayList<>(batchedMarketOrders);
        batchedMarketOrders.clear();
        
        for (BatchedOrder order : tempBatch) {
            if (order.isOpen) {
                if (!marketOpen(order.symbol, order.isLong, order.quantity)) {
                    accInfo.ordersWithErrors.put(order.orderId,
                            new ExchangeException(1, "Not enough margin to open", "Backtest"));
                }
                
            } else {
                if (!marketClose(order.symbol, order.isLong, order.quantity)) {
                    accInfo.ordersWithErrors.put(order.orderId, new ExchangeException(2, "Error when closing", "Backtest"));
                }
            }
        }
        
        return accInfo;
    }
    
    @Override
    public void setHedgeMode() {
        
        // only hedge mode supported so it's already in hedge mode
    }
    
    @Override
    public void setLeverage(String symbol, int leverage) {
        
        accInfo.leverages.put(symbol, leverage);
        
    }
    
    @Override
    public void setCrossMargin(String symbol) {
        
        // only cross margin supported so all symbols are already in cross margin mode
    }
    
}
