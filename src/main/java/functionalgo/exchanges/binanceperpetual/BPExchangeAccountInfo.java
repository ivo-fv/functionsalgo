package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeAccountInfo;

public interface BPExchangeAccountInfo extends ExchangeAccountInfo {
    
    long getTimestamp();
    
    double getWalletBalance();
    
    int getLeverage(String symbol);
    
    double getTakerFee();
    
    double getMarginBalance();
    
    double getQuantity(String symbol);
    
    double getAverageOpenPrice(String symbol);
    
    double getCurrentPrice(String symbol);
    
    long getNextFundingTime();
    
    double getFundingRate(String symbol);
    
    double getWorstMarginBalance();
}
