package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BinancePerpetualExchange extends Exchange {
    
    @Override
    BinancePerpetualExchangeAccountInfo getAccountInfo(long timestamp);
}
