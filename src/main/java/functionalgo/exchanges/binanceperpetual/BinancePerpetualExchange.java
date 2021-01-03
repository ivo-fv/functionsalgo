package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BinancePerpetualExchange extends Exchange {
    
    long getNextFundingTime();
    
    double getLongOpenPrice(String longSymbol);
    
    double getLongQuantity(String longSymbol);
    
    double getShortOpenPrice(String shortSymbol);
    
    double getShortQuantity(String shortSymbol);
    
}
