package functionalgo.exchanges.binanceperpetual;

import java.util.HashMap;
import java.util.Map;

import functionalgo.dataproviders.binanceperpetual.BPHistoricFundingRates;
import functionalgo.dataproviders.binanceperpetual.BPHistoricKlines;

public class BPSimExchange implements BPExchange {
    
    private class PositionData {
        
        private double openPrice;
        private double currPrice;
        private double quantity;
        private double initialMargin;
        private double pnl; // not with mark price
        private double totalFundingFees;
        
        private PositionData(double openPrice, double quantity, double initialMargin) {
            
            this.openPrice = openPrice;
            this.currPrice = openPrice;
            this.quantity = quantity;
            this.initialMargin = initialMargin;
            this.totalFundingFees = 0;
        }
    }
    
    private class AccountInfo implements BPExchangeAccountInfo {
        
        private double marginBalance;
        private double walletBalance;
        private HashMap<String, PositionData> longPositions;
        private HashMap<String, PositionData> shortPositions;
        private long lastUpdatedTime;
        
        private AccountInfo(double walletBalance) {
            
            this.walletBalance = walletBalance;
            marginBalance = walletBalance;
            
            longPositions = new HashMap<>();
            shortPositions = new HashMap<>();
            
            lastUpdatedTime = 0;
        }
        
        private void updateAccountInfo(long timestamp) {
            
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
        
        private void calculateMarginBalanceAndUpdatePositionData(long timestamp) {
            
            marginBalance = walletBalance;
            
            for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
                
                // should use a mark price for more accuracy
                double currPrice = bPHistoricKlines.getOpen(entry.getKey(), timestamp);
                entry.getValue().currPrice = currPrice;
                double pnl = (entry.getValue().currPrice - entry.getValue().openPrice) * entry.getValue().quantity;
                entry.getValue().pnl = pnl;
                marginBalance += pnl;
                
            }
            for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
                
                // should use a mark price for more accuracy
                double currPrice = bPHistoricKlines.getOpen(entry.getKey(), timestamp);
                entry.getValue().currPrice = currPrice;
                double pnl = (entry.getValue().openPrice - entry.getValue().currPrice) * entry.getValue().quantity;
                entry.getValue().pnl = pnl;
                marginBalance += pnl;
            }
            
            if (timestamp >= nextFundingTime) {
                for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
                    
                    double fundingRate = bPHistoricFundingRates.getRate(entry.getKey(), timestamp);
                    double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                    marginBalance -= funding;
                    walletBalance -= funding;
                    entry.getValue().totalFundingFees += funding;
                }
                
                for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
                    
                    double fundingRate = bPHistoricFundingRates.getRate(entry.getKey(), timestamp);
                    double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                    marginBalance += funding;
                    walletBalance += funding;
                    entry.getValue().totalFundingFees -= funding;
                }
            }
        }
        
        private void checkMaintenanceMargin() {
            
            double totalInitialMargin = 0;
            
            for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
                
                totalInitialMargin += entry.getValue().initialMargin;
                
            }
            for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
                
                totalInitialMargin += entry.getValue().initialMargin;
                
            }
            
            if (marginBalance <= totalInitialMargin * LIQUIDATION_PERCENT) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("!!! PROBABLY GOING TO GET LIQUIDATED !!!");
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                
                marginBalance = 0;
                walletBalance = 0;
                longPositions = new HashMap<>();
                shortPositions = new HashMap<>();
            }
        }
    }
    
    private static final long FUNDING_INTERVAL_MILLIS = 28800000; // 8 hours
    private static final long UPDATE_INTERVAL_MILLIS = 300000; // 5 minutes TODO change to 1 minute
    private static final double LIQUIDATION_PERCENT = 0.03;
    
    public static final double TAKER_OPEN_CLOSE_FEE = 0.0004;
    public static final double MAKER_OPEN_CLOSE_FEE = 0.0002;
    
    private AccountInfo accInfo;
    
    private long nextFundingTime;
    
    private BPHistoricKlines bPHistoricKlines;
    private BPHistoricFundingRates bPHistoricFundingRates;
    
    // TODO use more accurate information for maintenance and initial margin and symbol price and quantity
    // TODO buy() sell()
    
    public BPSimExchange(double walletBalance) {
        
        accInfo = new AccountInfo(walletBalance);
        
        bPHistoricKlines = BPHistoricKlines.loadKlines(BPHistoricKlines.KLINES_FILE);
        bPHistoricFundingRates = BPHistoricFundingRates.loadFundingRates(BPHistoricFundingRates.FUND_RATES_FILE);
    }
    
    @Override
    public BPExchangeAccountInfo getAccountInfo(long timestamp) {
        
        accInfo.updateAccountInfo(timestamp);
        
        return accInfo;
    }
    
}
