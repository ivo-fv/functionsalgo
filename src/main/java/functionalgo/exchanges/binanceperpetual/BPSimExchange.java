package functionalgo.exchanges.binanceperpetual;

import java.util.HashMap;
import java.util.Map;

import functionalgo.dataproviders.binanceperpetual.BPHistoricFundingRates;
import functionalgo.dataproviders.binanceperpetual.BPHistoricKlines;

public class BPSimExchange implements BPExchange {
    
    private class PositionData {
        
        @SuppressWarnings("unused")
        private static final String MARGIN_TYPE = "CROSSED"; // 8 hours
        
        double avgOpenPrice;
        double currPrice;
        double quantity;
        double initialMargin;
        double pnl; // not with mark price
        double totalFundingFees;
        
        PositionData(double avgOpenPrice, double quantity, double initialMargin) {
            
            this.avgOpenPrice = avgOpenPrice;
            this.currPrice = avgOpenPrice;
            this.quantity = quantity;
            this.initialMargin = initialMargin;
            this.pnl = 0;
            this.totalFundingFees = 0;
        }
    }
    
    private class AccountInfo implements BPExchangeAccountInfo {
        
        double marginBalance;
        double walletBalance;
        double marginUsed;
        HashMap<String, Short> leverages;
        HashMap<String, PositionData> longPositions;
        HashMap<String, PositionData> shortPositions;
        long lastUpdatedTime;
        
        AccountInfo(double walletBalance) {
            
            this.walletBalance = walletBalance;
            marginBalance = walletBalance;
            marginUsed = 0;
            
            longPositions = new HashMap<>();
            shortPositions = new HashMap<>();
            leverages = new HashMap<>();
            
            lastUpdatedTime = 0;
        }
        
        void updateAccountInfo(long timestamp) {
            
            if (timestamp <= lastUpdatedTime) {
                throw new IllegalArgumentException("timestamp must be larger than the previous");
            }
            
            if (lastUpdatedTime != 0) {
                
                for (long time = lastUpdatedTime + UPDATE_INTERVAL_MILLIS; time <= timestamp; time += UPDATE_INTERVAL_MILLIS) {
                    
                    calculateMarginBalanceAndUpdatePositionData(time);
                    checkMaintenanceMargin();
                    
                    // TODO simrandom downtime, sim random position close
                    
                    if (time >= nextFundingTime) {
                        nextFundingTime += FUNDING_INTERVAL_MILLIS;
                    }
                }
                // TODO in debug make sure lastUpdatedTime is sequential on every proper updateAccountInfo() call
                // ie: it's the same as the last time passed to the methods inside the loop
                lastUpdatedTime = (timestamp / UPDATE_INTERVAL_MILLIS) * UPDATE_INTERVAL_MILLIS;
                
            } else {
                // TODO make sure in debug that (timestamp / FUNDING_INTERVAL_MILLIS) is not a decimal
                nextFundingTime = ((timestamp / FUNDING_INTERVAL_MILLIS) * FUNDING_INTERVAL_MILLIS) + FUNDING_INTERVAL_MILLIS;
                
                calculateMarginBalanceAndUpdatePositionData(timestamp);
                checkMaintenanceMargin();
                
                lastUpdatedTime = (timestamp / UPDATE_INTERVAL_MILLIS) * UPDATE_INTERVAL_MILLIS;
            }
            
        }
        
        void calculateMarginBalanceAndUpdatePositionData(long timestamp) {
            
            marginBalance = walletBalance;
            
            for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
                
                // should use a mark price for more accuracy
                double currPrice = bpHistoricKlines.getOpen(entry.getKey(), timestamp);
                entry.getValue().currPrice = currPrice;
                double pnl = (entry.getValue().currPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
                entry.getValue().pnl = pnl;
                marginBalance += pnl;
                
            }
            for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
                
                // should use a mark price for more accuracy
                double currPrice = bpHistoricKlines.getOpen(entry.getKey(), timestamp);
                entry.getValue().currPrice = currPrice;
                double pnl = (entry.getValue().avgOpenPrice - entry.getValue().currPrice) * entry.getValue().quantity;
                entry.getValue().pnl = pnl;
                marginBalance += pnl;
            }
            
            if (timestamp >= nextFundingTime) {
                for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
                    
                    double fundingRate = bpHistoricFundingRates.getRate(entry.getKey(), timestamp);
                    double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                    marginBalance -= funding;
                    walletBalance -= funding;
                    entry.getValue().totalFundingFees += funding;
                }
                
                for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
                    
                    double fundingRate = bpHistoricFundingRates.getRate(entry.getKey(), timestamp);
                    double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                    marginBalance += funding;
                    walletBalance += funding;
                    entry.getValue().totalFundingFees -= funding;
                }
            }
        }
        
        void checkMaintenanceMargin() {
            
            // assuming crossed margin type
            double highestInitialMargin = 0;
            
            for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
                
                highestInitialMargin = Math.max(highestInitialMargin, entry.getValue().initialMargin);
            }
            for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
                
                highestInitialMargin = Math.max(highestInitialMargin, entry.getValue().initialMargin);
            }
            
            // the maintenance margin is *always less* than 50% of the initial margin
            if (marginBalance <= highestInitialMargin / 2) {
                // TODO log to exchange about possible liq
                
                marginBalance = 0;
                walletBalance = 0;
                longPositions = new HashMap<>();
                shortPositions = new HashMap<>();
            }
        }
        
        @Override
        public double getLongClosePriceWithSlippage(String symbol, double notionalValue) {
            
            return longPositions.get(symbol).currPrice * (1 - (slippageModel.getSlippage(notionalValue, symbol) - 1));
        }
        
        @Override
        public double getShortClosePriceWithSlippage(String symbol, double notionalValue) {
            
            return shortPositions.get(symbol).currPrice * slippageModel.getSlippage(notionalValue, symbol);
        }
        
        @Override
        public double getLongQty(String symbol) {
            
            return longPositions.get(symbol).quantity;
        }
        
        @Override
        public double getShortQty(String symbol) {
            
            return shortPositions.get(symbol).quantity;
        }
        
        @Override
        public String[] getLongPositionSymbols() {
            
            return (String[]) longPositions.keySet().toArray();
        }
        
        @Override
        public String[] getShortPositionSymbols() {
            
            return (String[]) shortPositions.keySet().toArray();
        }
    }
    
    @SuppressWarnings("unused")
    private static final boolean IS_HEDGE_MODE = true;
    
    private static final long FUNDING_INTERVAL_MILLIS = 28800000; // 8 hours
    private static final long UPDATE_INTERVAL_MILLIS = 300000; // 5 minutes TODO change to 1 minute
    private static final double OPEN_LOSS_SIM_MULT = 1.06; // will probably never be this high
    
    public static final double TAKER_OPEN_CLOSE_FEE = 0.0004;
    public static final double MAKER_OPEN_CLOSE_FEE = 0.0002;
    
    private AccountInfo accInfo;
    
    private long nextFundingTime;
    private short defaultLeverage;
    
    private BPHistoricKlines bpHistoricKlines;
    private BPHistoricFundingRates bpHistoricFundingRates;
    private BPSlippageModel slippageModel;
    
    // TODO use more accurate information for maintenance and initial margin and symbol price and quantity
    // TODO simulate sub-accounts, ie: decompose positions to those of separate strategies
    
    public BPSimExchange(double walletBalance, short defaultLeverage) {
        
        accInfo = new AccountInfo(walletBalance);
        this.defaultLeverage = defaultLeverage;
        bpHistoricKlines = BPHistoricKlines.loadKlines(BPHistoricKlines.KLINES_FILE);
        bpHistoricFundingRates = BPHistoricFundingRates.loadFundingRates(BPHistoricFundingRates.FUND_RATES_FILE);
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
    
    private void openPosition(String symbol, double symbolQty, boolean isLong) {
        
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
        
        if (accInfo.marginUsed + (initialMargin * OPEN_LOSS_SIM_MULT) >= balanceToCheck) {
            // TODO log that order can't be executed due to insuficient initial margin
            return;
        }
        
        // TODO try with initialMargin * OPEN_LOSS_SIM_MULT
        accInfo.marginUsed += initialMargin;
        
        double openFee = notionalValue * TAKER_OPEN_CLOSE_FEE;
        accInfo.marginBalance -= openFee;
        accInfo.walletBalance -= openFee;
        
        if (isLong) {
            if (accInfo.longPositions.containsKey(symbol)) {
                accInfo.longPositions.get(symbol).initialMargin += initialMargin;
                double newQty = accInfo.longPositions.get(symbol).quantity + symbolQty;
                double percOldQty = accInfo.longPositions.get(symbol).quantity / newQty;
                double prevAvgOpenPrice = accInfo.longPositions.get(symbol).avgOpenPrice;
                double percNewQty = symbolQty / newQty;
                accInfo.longPositions.get(symbol).avgOpenPrice = (prevAvgOpenPrice * percOldQty) + (openPrice * percNewQty);
                accInfo.longPositions.get(symbol).quantity = newQty;
            } else {
                accInfo.longPositions.put(symbol, new PositionData(openPrice, symbolQty, initialMargin));
            }
        } else {
            if (accInfo.shortPositions.containsKey(symbol)) {
                accInfo.shortPositions.get(symbol).initialMargin += initialMargin;
                double newQty = accInfo.shortPositions.get(symbol).quantity + symbolQty;
                double percOldQty = accInfo.shortPositions.get(symbol).quantity / newQty;
                double prevAvgOpenPrice = accInfo.shortPositions.get(symbol).avgOpenPrice;
                double percNewQty = symbolQty / newQty;
                accInfo.shortPositions.get(symbol).avgOpenPrice = (prevAvgOpenPrice * percOldQty) + (openPrice * percNewQty);
                accInfo.shortPositions.get(symbol).quantity = newQty;
            } else {
                accInfo.shortPositions.put(symbol, new PositionData(openPrice, symbolQty, initialMargin));
            }
        }
    }
    
    @Override
    public void marketOpenLong(String symbol, double symbolQty) {
        
        openPosition(symbol, symbolQty, true);
    }
    
    @Override
    public void marketOpenShort(String symbol, double symbolQty) {
        
        openPosition(symbol, symbolQty, false);
        
    }
    
    @Override
    public void marketCloseLong(String symbol) {
        
        if (accInfo.longPositions.containsKey(symbol)) {
            
            double nValSlippage = accInfo.longPositions.get(symbol).quantity * accInfo.longPositions.get(symbol).currPrice;
            double closePrice = accInfo.getLongClosePriceWithSlippage(symbol, nValSlippage);
            double notionalValue = closePrice * accInfo.longPositions.get(symbol).quantity;
            double pnl = (closePrice - accInfo.longPositions.get(symbol).avgOpenPrice)
                    * accInfo.longPositions.get(symbol).quantity;
            double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;
            
            accInfo.walletBalance -= closeFee;
            accInfo.marginBalance -= closeFee;
            accInfo.walletBalance += pnl;
            
            accInfo.longPositions.remove(symbol);
            // TODO log successful long close
        } else {
            // TODO log no long position with symbol
        }
    }
    
    @Override
    public void marketCloseShort(String symbol) {
        
        if (accInfo.shortPositions.containsKey(symbol)) {
            
            double nValSlippage = accInfo.shortPositions.get(symbol).quantity * accInfo.shortPositions.get(symbol).currPrice;
            double closePrice = accInfo.getShortClosePriceWithSlippage(symbol, nValSlippage);
            double notionalValue = closePrice * accInfo.shortPositions.get(symbol).quantity;
            double pnl = (accInfo.shortPositions.get(symbol).avgOpenPrice - closePrice)
                    * accInfo.shortPositions.get(symbol).quantity;
            double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;
            
            accInfo.walletBalance -= closeFee;
            accInfo.marginBalance -= closeFee;
            accInfo.walletBalance += pnl;
            
            accInfo.shortPositions.remove(symbol);
            // TODO log successful short close
        } else {
            // TODO log no short position with symbol
        }
    }
    
    @Override
    public void marketReduceLong(String symbol, double symbolQty) {
        
        if (accInfo.longPositions.containsKey(symbol)) {
            if (accInfo.longPositions.get(symbol).quantity > symbolQty) {
                
                double nValSlippage = accInfo.longPositions.get(symbol).quantity * accInfo.longPositions.get(symbol).currPrice;
                double closePrice = accInfo.getLongClosePriceWithSlippage(symbol, nValSlippage);
                double notionalValue = closePrice * symbolQty;
                double pnl = (closePrice - accInfo.longPositions.get(symbol).avgOpenPrice) * symbolQty;
                double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;
                
                accInfo.walletBalance -= closeFee;
                accInfo.marginBalance -= closeFee;
                accInfo.walletBalance += pnl;
                
                double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol) : defaultLeverage;
                double initialMargin = notionalValue / leverage;
                accInfo.longPositions.get(symbol).initialMargin -= initialMargin;
                accInfo.longPositions.get(symbol).pnl -= pnl;
                accInfo.longPositions.get(symbol).quantity -= symbolQty;
                
                // TODO log successful long close
            } else {
                marketCloseLong(symbol);
            }
        } else {
            // TODO log no long position with symbol
        }
    }
    
    @Override
    public void marketReduceShort(String symbol, double symbolQty) {
        
        if (accInfo.shortPositions.containsKey(symbol)) {
            if (accInfo.shortPositions.get(symbol).quantity > symbolQty) {
                
                double nValSlippage = accInfo.shortPositions.get(symbol).quantity * accInfo.shortPositions.get(symbol).currPrice;
                double closePrice = accInfo.getShortClosePriceWithSlippage(symbol, nValSlippage);
                double notionalValue = closePrice * symbolQty;
                double pnl = (accInfo.shortPositions.get(symbol).avgOpenPrice - closePrice) * symbolQty;
                double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;
                
                accInfo.walletBalance -= closeFee;
                accInfo.marginBalance -= closeFee;
                accInfo.walletBalance += pnl;
                
                double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol) : defaultLeverage;
                double initialMargin = notionalValue / leverage;
                accInfo.shortPositions.get(symbol).initialMargin -= initialMargin;
                accInfo.shortPositions.get(symbol).pnl -= pnl;
                accInfo.shortPositions.get(symbol).quantity -= symbolQty;
                
                // TODO log successful short close
            } else {
                marketCloseShort(symbol);
            }
        } else {
            // TODO log no short position with symbol
        }
    }
}
