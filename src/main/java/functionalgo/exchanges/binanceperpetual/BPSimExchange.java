package functionalgo.exchanges.binanceperpetual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import functionalgo.dataproviders.binanceperpetual.BPHistoricFundingRates;
import functionalgo.dataproviders.binanceperpetual.BPHistoricKlines;
import functionalgo.dataproviders.binanceperpetual.Interval;
import functionalgo.exceptions.ExchangeException;

public class BPSimExchange implements BPExchange {
    
    private class PositionData {
        
        @SuppressWarnings("unused")
        private static final String MARGIN_TYPE = "CROSSED";
        
        String symbol;
        boolean isLong;
        double avgOpenPrice;
        double currPrice;
        double quantity;
        double margin;
        
        PositionData(String symbol, boolean isLong, double avgOpenPrice, double quantity, double initialMargin) {
            
            this.symbol = symbol;
            this.isLong = isLong;
            this.avgOpenPrice = avgOpenPrice;
            this.currPrice = avgOpenPrice;
            this.quantity = quantity;
            this.margin = initialMargin;
        }
    }
    
    private class AccountInfo implements BPExchangeAccountInfo {
        
        double marginBalance;
        double walletBalance;
        HashMap<String, Short> leverages;
        HashMap<String, PositionData> positions;
        long lastUpdatedTime;
        double takerFee = TAKER_OPEN_CLOSE_FEE;
        private double worstMarginBalance;
        public HashMap<String, ExchangeException> ordersWithErrors;
        
        AccountInfo(double walletBalance) {
            
            this.walletBalance = walletBalance;
            marginBalance = walletBalance;
            worstMarginBalance = walletBalance;
            
            positions = new HashMap<>();
            leverages = new HashMap<>();
            ordersWithErrors = new HashMap<>();
            
            lastUpdatedTime = 0;
        }
        
        void updateAccountInfo(long timestamp) {
            
            // TODO random position adl close, simulate not being able to trade for a period of time
            if (timestamp <= lastUpdatedTime) {
                throw new IllegalArgumentException("timestamp must be larger than the previous");
            }
            
            if (lastUpdatedTime != 0) {
                
                for (long time = lastUpdatedTime + updateIntervalMillis; time <= timestamp; time += updateIntervalMillis) {
                    
                    calculateMarginBalanceAndUpdatePositionData(time);
                    checkMaintenanceMargin(time);
                    
                    if (time >= nextFundingTime) {
                        nextFundingTime += fundingIntervalMillis;
                    }
                }
                
                lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
            } else {
                nextFundingTime = ((timestamp / fundingIntervalMillis) * fundingIntervalMillis) + fundingIntervalMillis;
                
                calculateMarginBalanceAndUpdatePositionData(timestamp);
                checkMaintenanceMargin(timestamp);
                
                lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
            }
            
        }
        
        void calculateMarginBalanceAndUpdatePositionData(long timestamp) {
            
            marginBalance = walletBalance;
            worstMarginBalance = walletBalance;
            
            for (Map.Entry<String, PositionData> entry : positions.entrySet()) {
                // use mark price for more accuracy
                if (entry.getValue().isLong) {
                    double currPrice = bpHistoricKlines.getOpen(entry.getValue().symbol, timestamp);
                    entry.getValue().currPrice = currPrice;
                    double pnl = (entry.getValue().currPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
                    marginBalance += pnl;
                    double worstPrice = bpHistoricKlines.getLow(entry.getValue().symbol, timestamp);
                    double worstPnL = (worstPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
                    worstMarginBalance += worstPnL;
                    short leverage = leverages.containsKey(entry.getValue().symbol) ? leverages.get(entry.getValue().symbol)
                            : defaultLeverage;
                    entry.getValue().margin = (entry.getValue().currPrice * entry.getValue().quantity) / leverage;
                } else {
                    double currPrice = bpHistoricKlines.getOpen(entry.getValue().symbol, timestamp);
                    entry.getValue().currPrice = currPrice;
                    double pnl = (entry.getValue().avgOpenPrice - entry.getValue().currPrice) * entry.getValue().quantity;
                    marginBalance += pnl;
                    double worstPrice = bpHistoricKlines.getHigh(entry.getValue().symbol, timestamp);
                    double worstPnL = (entry.getValue().avgOpenPrice - worstPrice) * entry.getValue().quantity;
                    worstMarginBalance += worstPnL;
                    short leverage = leverages.containsKey(entry.getValue().symbol) ? leverages.get(entry.getValue().symbol)
                            : defaultLeverage;
                    entry.getValue().margin = (entry.getValue().currPrice * entry.getValue().quantity) / leverage;
                }
            }
            
            if (timestamp >= nextFundingTime) {
                for (Map.Entry<String, PositionData> entry : positions.entrySet()) {
                    if (entry.getValue().isLong) {
                        double fundingRate = bpHistoricFundingRates.getRate(entry.getValue().symbol, timestamp);
                        double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                        marginBalance -= funding;
                        walletBalance -= funding;
                        worstMarginBalance -= funding;
                    } else {
                        double fundingRate = bpHistoricFundingRates.getRate(entry.getValue().symbol, timestamp);
                        double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                        marginBalance += funding;
                        walletBalance += funding;
                        worstMarginBalance += funding;
                    }
                }
            }
        }
        
        void checkMaintenanceMargin(long time) {
            
            // assuming crossed margin type
            double highestInitialMargin = 0;
            
            for (Map.Entry<String, PositionData> entry : positions.entrySet()) {
                
                highestInitialMargin = Math.max(highestInitialMargin, entry.getValue().margin);
            }
            // the maintenance margin is *always less* than 50% of the initial margin
            if (worstMarginBalance <= highestInitialMargin / 2) {
                // TODO log to exchange about possible liq
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("!!! Probably going to get liquidated at: " + time);
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                marginBalance = 0;
                walletBalance = 0;
                positions = new HashMap<>();
            }
        }
        
        @Override
        public long getTimestamp() {
            
            return lastUpdatedTime;
        }
        
        @Override
        public double getWalletBalance() {
            
            return walletBalance;
        }
        
        @Override
        public double getQuantity(String symbol, boolean isLong) {
            
            return positions.get(getPositionId(symbol, isLong)).quantity;
        }
        
        @Override
        public int getLeverage(String symbol) {
            
            return leverages.get(symbol);
        }
        
        @Override
        public double getTakerFee() {
            
            return takerFee;
        }
        
        @Override
        public double getMarginBalance() {
            
            return marginBalance;
        }
        
        @Override
        public double getAverageOpenPrice(String symbol, boolean isLong) {
            
            return positions.get(getPositionId(symbol, isLong)).avgOpenPrice;
        }
        
        @Override
        public double getWorstMarginBalance() {
            
            return worstMarginBalance;
        }
        
        @Override
        public long getNextFundingTime(String symbol) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getFundingRate(String symbol) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getMarkPrice(String symbol) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public ExchangeException getOrderError(String orderId) {
            
            return ordersWithErrors.get(orderId);
        }
        
        @Override
        public boolean isBalancesDesynch() {
            
            return false;
        }
    }
    
    @SuppressWarnings("unused")
    private static final boolean IS_HEDGE_MODE = true;
    
    private static final double OPEN_LOSS_SIM_MULT = 1.06; // will probably never be this high
    
    public static final double TAKER_OPEN_CLOSE_FEE = 0.0004;
    public static final double MAKER_OPEN_CLOSE_FEE = 0.0002;
    
    private AccountInfo accInfo;
    
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
        
        accInfo = new AccountInfo(walletBalance);
        this.defaultLeverage = defaultLeverage;
        updateIntervalMillis = updateInterval.toMilliseconds();
        bpHistoricKlines = BPHistoricKlines.loadKlines(updateInterval);
        bpHistoricFundingRates = BPHistoricFundingRates.loadFundingRates();
        fundingIntervalMillis = bpHistoricFundingRates.getFundingIntervalMillis();
        slippageModel = BPSlippageModel.LoadSlippageModel(BPSlippageModel.MODEL_FILE);
        batchedMarketOrders = new ArrayList<>();
    }
    
    @Override
    public BPExchangeAccountInfo getAccountInfo(long timestamp) {
        
        accInfo.updateAccountInfo(timestamp);
        
        return accInfo;
    }
    
    private boolean marketOpen(String symbol, boolean isLong, double symbolQty) {
        
        String positionId = getPositionId(symbol, isLong);
        
        double openPrice = bpHistoricKlines.getOpen(symbol, accInfo.lastUpdatedTime);
        double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol) : defaultLeverage;
        double initialMargin = (openPrice * symbolQty) / leverage;
        if (isLong) {
            openPrice *= slippageModel.getSlippage(openPrice * symbolQty, symbol);
        } else {
            openPrice *= 1 - (slippageModel.getSlippage(openPrice * symbolQty, symbol) - 1);
        }
        double notionalValue = openPrice * symbolQty;
        
        double marginUsed = 0;
        for (Map.Entry<String, PositionData> entry : accInfo.positions.entrySet()) {
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
            accInfo.positions.put(positionId, new PositionData(symbol, isLong, openPrice, symbolQty, initialMargin));
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
    
    private String getPositionId(String symbol, boolean isLong) {
        
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
    public BPExchangeAccountInfo executeBatchedOrders() {
        
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
    
}
