package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeAccountInfo;

public interface BPExchangeAccountInfo extends ExchangeAccountInfo {
    
    long getTimestamp();
    
    double getWalletBalance();
    
    int getLeverage(String symbol);
    
    double getTakerFee();
    
    double getMarginBalance();
    
    double getQuantity(String symbol, boolean isLong);
    
    double getAverageOpenPrice(String symbol, boolean isLong);
    
    long getNextFundingTime(String symbol);
    
    double getFundingRate(String symbol);
    
    double getWorstMarginBalance();
    
    double getMarkPrice(String symbol);
}
