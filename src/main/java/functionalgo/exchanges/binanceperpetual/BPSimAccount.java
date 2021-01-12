package functionalgo.exchanges.binanceperpetual;

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
    HashMap<String, Short> leverages;
    HashMap<String, PositionData> positions;
    long lastUpdatedTime;
    double takerFee = BPSimExchange.TAKER_OPEN_CLOSE_FEE;
    double worstMarginBalance;
    public HashMap<String, ExchangeException> ordersWithErrors;
    
    BPSimAccount(double walletBalance) {
        
        this.walletBalance = walletBalance;
        marginBalance = walletBalance;
        worstMarginBalance = walletBalance;
        
        positions = new HashMap<>();
        leverages = new HashMap<>();
        ordersWithErrors = new HashMap<>();
        
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
