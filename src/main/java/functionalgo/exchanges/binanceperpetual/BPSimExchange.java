package functionalgo.exchanges.binanceperpetual;

import java.util.HashMap;
import java.util.Map;

import functionalgo.dataproviders.binanceperpetual.BPHistoricFundingRates;
import functionalgo.dataproviders.binanceperpetual.BPHistoricKlines;
import functionalgo.dataproviders.binanceperpetual.Interval;

// TODO executor merge with exchange, execution strat on strat
public class BPSimExchange implements BPExchange {
    
    private class PositionData {
        
        @SuppressWarnings("unused")
        private static final String MARGIN_TYPE = "CROSSED"; // 8 hours
        
        String symbol;
        boolean isLong;
        double avgOpenPrice;
        double currPrice;
        double quantity;
        double initialMargin;
        double totalFundingFees;
        
        PositionData(String symbol, boolean isLong, double avgOpenPrice, double quantity, double initialMargin) {
            
            this.symbol = symbol;
            this.isLong = isLong;
            this.avgOpenPrice = avgOpenPrice;
            this.currPrice = avgOpenPrice;
            this.quantity = quantity;
            this.initialMargin = initialMargin;
            this.totalFundingFees = 0;
        }
    }
    
    private class AccountInfo implements BPExchangeAccountInfo {
        
        double marginBalance;
        double walletBalance;
        HashMap<String, Short> leverages;
        HashMap<String, PositionData> positions;
        long lastUpdatedTime;
        double takerFee = TAKER_OPEN_CLOSE_FEE;
        
        AccountInfo(double walletBalance) {
            
            this.walletBalance = walletBalance;
            marginBalance = walletBalance;
            
            positions = new HashMap<>();
            leverages = new HashMap<>();
            
            lastUpdatedTime = 0;
        }
        
        void updateAccountInfo(long timestamp) {
            
            if (timestamp <= lastUpdatedTime) {
                throw new IllegalArgumentException("timestamp must be larger than the previous");
            }
            
            if (lastUpdatedTime != 0) {
                
                for (long time = lastUpdatedTime + updateIntervalMillis; time <= timestamp; time += updateIntervalMillis) {
                    
                    calculateMarginBalanceAndUpdatePositionData(time);
                    checkMaintenanceMargin();
                    
                    // TODO simrandom downtime, sim random position close
                    
                    if (time >= nextFundingTime) {
                        nextFundingTime += fundingIntervalMillis;
                    }
                }
                // TODO in debug make sure lastUpdatedTime is sequential on every proper updateAccountInfo() call
                // ie: it's the same as the last time passed to the methods inside the loop
                lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
                
            } else {
                // TODO make sure in debug that (timestamp / FUNDING_INTERVAL_MILLIS) is not a decimal
                nextFundingTime = ((timestamp / fundingIntervalMillis) * fundingIntervalMillis) + fundingIntervalMillis;
                
                calculateMarginBalanceAndUpdatePositionData(timestamp);
                checkMaintenanceMargin();
                
                lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
            }
            
        }
        
        void calculateMarginBalanceAndUpdatePositionData(long timestamp) {
            
            marginBalance = walletBalance;
            // TODO refactor to use positions.values...
            for (Map.Entry<String, PositionData> entry : positions.entrySet()) {
                // use mark price for more accuracy
                if (entry.getValue().isLong) {
                    double currPrice = bpHistoricKlines.getOpen(entry.getValue().symbol, timestamp);
                    entry.getValue().currPrice = currPrice;
                    double pnl = (entry.getValue().currPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
                    marginBalance += pnl;
                } else {
                    double currPrice = bpHistoricKlines.getOpen(entry.getValue().symbol, timestamp);
                    entry.getValue().currPrice = currPrice;
                    double pnl = (entry.getValue().avgOpenPrice - entry.getValue().currPrice) * entry.getValue().quantity;
                    marginBalance += pnl;
                }
            }
            // TODO refactor to use positions.values...
            if (timestamp >= nextFundingTime) {
                for (Map.Entry<String, PositionData> entry : positions.entrySet()) {
                    if (entry.getValue().isLong) {
                        double fundingRate = bpHistoricFundingRates.getRate(entry.getValue().symbol, timestamp);
                        double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                        marginBalance -= funding;
                        walletBalance -= funding;
                        entry.getValue().totalFundingFees += funding;
                    } else {
                        double fundingRate = bpHistoricFundingRates.getRate(entry.getValue().symbol, timestamp);
                        double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                        marginBalance += funding;
                        walletBalance += funding;
                        entry.getValue().totalFundingFees -= funding;
                    }
                }
            }
        }
        
        void checkMaintenanceMargin() {
            
            // assuming crossed margin type
            double highestInitialMargin = 0;
            
            for (Map.Entry<String, PositionData> entry : positions.entrySet()) {
                
                highestInitialMargin = Math.max(highestInitialMargin, entry.getValue().initialMargin);
            }
            // the maintenance margin is *always less* than 50% of the initial margin
            if (marginBalance <= highestInitialMargin / 2) {
                // TODO log to exchange about possible liq
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("!!! Probably going to get liquidated !!!");
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                marginBalance = 0;
                walletBalance = 0;
                positions = new HashMap<>();
            }
        }
        
        @Override
        public long getLastUpdateTimestamp() {
            
            return lastUpdatedTime;
        }
        
        @Override
        public double getWalletBalance() {
            
            return walletBalance;
        }
        
        @Override
        public double getQty(String positionId) {
            
            return positions.get(positionId).quantity;
        }
        
        @Override
        public double getInitialMargin(String positionId) {
            
            return positions.get(positionId).initialMargin;
        }
        
        @Override
        public double getMarketClosePnL(String positionId, double qtyToClose) {
            
            String symbol = accInfo.positions.get(positionId).symbol;
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
            
            return pnl;
        }
        
        @Override
        public int getLeverage(String symbol) {
            
            return leverages.get(symbol);
        }
        
        @Override
        public double getTotalFundingFees(String positionId) {
            
            return positions.get(positionId).totalFundingFees;
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
        public double getOpenPrice(String positionId) {
            
            return positions.get(positionId).avgOpenPrice;
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
    
    // TODO use more accurate information for maintenance and initial margin and symbol price and quantity
    
    public BPSimExchange(double walletBalance, short defaultLeverage, Interval updateInterval) {
        
        accInfo = new AccountInfo(walletBalance);
        this.defaultLeverage = defaultLeverage;
        updateIntervalMillis = updateInterval.toMilliseconds();
        bpHistoricKlines = BPHistoricKlines.loadKlines(updateInterval);
        bpHistoricFundingRates = BPHistoricFundingRates.loadFundingRates();
        fundingIntervalMillis = bpHistoricFundingRates.getFundingIntervalMillis();
        slippageModel = BPSlippageModel.LoadSlippageModel(BPSlippageModel.MODEL_FILE);
    }
    
    @Override
    public BPExchangeAccountInfo getAccountInfo(long timestamp) {
        
        accInfo.updateAccountInfo(timestamp);
        
        return accInfo;
    }
    
    @Override
    public boolean isRandomFailingOrdersEnabled() {
        
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean marketOpen(String positionId, String symbol, boolean isLong, double symbolQty) {
        
        double openPrice = bpHistoricKlines.getOpen(symbol, accInfo.lastUpdatedTime);
        if (isLong) {
            openPrice *= slippageModel.getSlippage(openPrice * symbolQty, symbol);
        } else {
            openPrice *= 1 - (slippageModel.getSlippage(openPrice * symbolQty, symbol) - 1);
        }
        double notionalValue = openPrice * symbolQty;
        double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol) : defaultLeverage;
        double initialMargin = notionalValue / leverage;
        
        // TODO verify if it's checked against marginBalance or walletBalance
        double balanceToCheck = Math.min(accInfo.marginBalance, accInfo.walletBalance);
        double marginUsed = 0;
        for (Map.Entry<String, PositionData> entry : accInfo.positions.entrySet()) {
            // TODO try with += entry.getValue() * OPEN_LOSS_SIM_MULT
            marginUsed += entry.getValue().initialMargin;
        }
        if (marginUsed + (initialMargin * OPEN_LOSS_SIM_MULT) >= balanceToCheck) {
            // TODO log that order can't be executed due to insuficient initial margin
            System.out.println("Not enough margin to open position.");
            return false;
        }
        
        double openFee = notionalValue * TAKER_OPEN_CLOSE_FEE;
        accInfo.marginBalance -= openFee;
        accInfo.walletBalance -= openFee;
        
        if (accInfo.positions.containsKey(positionId)) {
            accInfo.positions.get(positionId).initialMargin += initialMargin;
            double newQty = accInfo.positions.get(positionId).quantity + symbolQty;
            double percOldQty = accInfo.positions.get(positionId).quantity / newQty;
            double prevAvgOpenPrice = accInfo.positions.get(positionId).avgOpenPrice;
            double percNewQty = symbolQty / newQty;
            accInfo.positions.get(positionId).avgOpenPrice = (prevAvgOpenPrice * percOldQty) + (openPrice * percNewQty);
            accInfo.positions.get(positionId).quantity = newQty;
        } else {
            accInfo.positions.put(positionId, new PositionData(symbol, isLong, openPrice, symbolQty, initialMargin));
        }
        // log successful open
        return true;
    }
    
    @Override
    public boolean marketClose(String positionId, double qtyToClose) {
        
        if (accInfo.positions.containsKey(positionId)) {
            
            String symbol = accInfo.positions.get(positionId).symbol;
            
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
                double initialMargin = notionalValue / leverage;
                accInfo.positions.get(positionId).initialMargin -= initialMargin;
                accInfo.positions.get(positionId).quantity -= qtyToClose;
                
                // TODO log successful long close
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
                
                // TODO log log successful long close
            }
        } else {
            // TODO log no position with positionId
            return false;
        }
        
        return true;
    }
    
}
