package functionalgo.exchanges.binanceperpetual;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SimBinancePerpetualExchange implements BinancePerpetualExchange {
    
    private class PositionData {
        
        private double openPrice;
        private double currPrice;
        private double quantity;
        private double initialMargin;
        
        public PositionData(double openPrice, double quantity, double initialMargin) {
            
            this.openPrice = openPrice;
            this.currPrice = openPrice;
            this.quantity = quantity;
            this.initialMargin = initialMargin;
        }
    }
    
    private static final long FUNDING_INTERVAL_MILLIS = 28800000; // 8 hours
    private static final long UPDATE_INTERVAL_MILLIS = 300000; // 5 minutes TODO change to 1 minute
    private static final double LIQUIDATION_PERCENT = 0.03;
    
    private double marginBalance;
    private double walletBalance;
    private HashMap<String, PositionData> longPositions;
    private HashMap<String, PositionData> shortPositions;
    private long lastUpdatedTime;
    private long nextFundingTime;
    
    private Klines klines;
    
    // TODO buy() sell()
    
    public SimBinancePerpetualExchange(double walletBalance) {
        
        this.walletBalance = walletBalance;
        marginBalance = walletBalance;
        
        longPositions = new HashMap<>();
        shortPositions = new HashMap<>();
        
        lastUpdatedTime = 0;
        
        klines = Klines.loadKlines();
    }
    
    @Override
    public void updateAccountInfo(long timestamp) {
        
        if (timestamp <= lastUpdatedTime) {
            throw new IllegalArgumentException("timestamp must be larger than the previous");
        }
        
        if (lastUpdatedTime != 0) {
            
            for (long time = lastUpdatedTime + UPDATE_INTERVAL_MILLIS; time <= timestamp; time += UPDATE_INTERVAL_MILLIS) {
                
                calculateMarginBalance(time);
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
            
            calculateMarginBalance(timestamp);
            checkMaintenanceMargin();
            
            lastUpdatedTime = (timestamp / UPDATE_INTERVAL_MILLIS) * UPDATE_INTERVAL_MILLIS;
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
    
    private void calculateMarginBalance(long timestamp) {
        
        marginBalance = walletBalance;
        
        for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
            
            // should use a mark price for more accuracy
            double currPrice = klines.getOpen(entry.getKey(), timestamp);
            
            entry.getValue().currPrice = currPrice;
            
            marginBalance += (entry.getValue().currPrice - entry.getValue().openPrice) * entry.getValue().quantity;
            
        }
        
        for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
            
            // should use a mark price for more accuracy
            double currPrice = klines.getOpen(entry.getKey(), timestamp);
            
            entry.getValue().currPrice = currPrice;
            
            marginBalance += (entry.getValue().openPrice - entry.getValue().currPrice) * entry.getValue().quantity;
        }
        
        if (timestamp >= nextFundingTime) {
            for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
                
                double fundingRate = fundingRates.get(timestamp).get(entry.getKey()).rate;
                double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                marginBalance -= funding;
                walletBalance -= funding;
            }
            
            for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
                
                double fundingRate = fundingRates.get(timestamp).get(entry.getKey()).rate;
                double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                marginBalance += funding;
                walletBalance += funding;
            }
        }
    }
    
    @Override
    public long getNextFundingTime() {
        
        return nextFundingTime;
    }
    
    @Override
    public double getLongOpenPrice(String longSymbol) {
        
        return longPositions.get(longSymbol).openPrice;
    }
    
    @Override
    public double getLongQuantity(String longSymbol) {
        
        return longPositions.get(longSymbol).quantity;
    }
    
    @Override
    public double getShortOpenPrice(String shortSymbol) {
        
        return shortPositions.get(shortSymbol).openPrice;
    }
    
    @Override
    public double getShortQuantity(String shortSymbol) {
        
        return shortPositions.get(shortSymbol).quantity;
    }
    
}
