package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BinancePerpetualExchange extends Exchange {

    int getLongPositionPnL(String longSymbol);

    int getShortPositionPnL(String shortSymbol);

    Object getNextFundingTime();
    
}
