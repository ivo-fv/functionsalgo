package functionalgo.binanceperpetual.exchange;

import java.util.HashMap;

import functionalgo.exceptions.ExchangeException;

class BPSimAccount implements BPAccount {
    
    class PositionData {
        
        static final String MARGIN_TYPE = "CROSSED";
        
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
    
    double marginBalance;
    double walletBalance;
    HashMap<String, Integer> leverages;
    HashMap<String, PositionData> positions;
    HashMap<String, Double> fundingRates;
    long lastUpdatedTime;
    public long nextFundingTime;
    double takerFee = BPSimExchange.TAKER_OPEN_CLOSE_FEE;
    double worstCurrentMarginBalance;
    HashMap<String, ExchangeException> ordersWithErrors;
    HashMap<String, Double> ordersWithQuantities;
    
    BPSimAccount(double walletBalance) {
        
        this.walletBalance = walletBalance;
        marginBalance = walletBalance;
        worstCurrentMarginBalance = walletBalance;
        
        positions = new HashMap<>();
        leverages = new HashMap<>();
        fundingRates = new HashMap<>();
        ordersWithErrors = new HashMap<>();
        ordersWithQuantities = new HashMap<>();
        
        lastUpdatedTime = 0;
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
        
        return positions.get(BPSimExchange.getPositionId(symbol, isLong)).quantity;
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
        
        return positions.get(BPSimExchange.getPositionId(symbol, isLong)).avgOpenPrice;
    }
    
    @Override
    public double getWorstCurrenttMarginBalance() {
        
        return worstCurrentMarginBalance;
    }
    
    @Override
    public long getNextFundingTime(String symbol) {
        
        return nextFundingTime;
    }
    
    @Override
    public double getFundingRate(String symbol) {
        
        return fundingRates.get(symbol);
    }
    
    @Override
    public double getMarkPrice(String symbol) {
        
        if (positions.containsKey(BPSimExchange.getPositionId(symbol, true))) {
            return positions.get(BPSimExchange.getPositionId(symbol, true)).currPrice;
        } else {
            return positions.get(BPSimExchange.getPositionId(symbol, false)).currPrice;
        }
    }
    
    @Override
    public ExchangeException getOrderError(String orderId) {
        
        return ordersWithErrors.get(orderId);
    }
    
    @Override
    public boolean isBalancesDesynch() {
        
        return false;
    }
    
    @Override
    public double getOrderQuantity(String orderId) {
        
        return ordersWithQuantities.get(orderId);
    }
    
    @Override
    public boolean isPositionsDesynch() {
        
        return false;
    }
    
    @Override
    public boolean isSymbolIsolated(String symbol) {
        
        return false;
    }
    
    @Override
    public boolean isHedgeMode() {
        
        return true;
    }
}
