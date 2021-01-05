package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BPExchange extends Exchange {
    
    @Override
    BPExchangeAccountInfo getAccountInfo(long timestamp);
    
    boolean isRandomFailingOrdersEnabled();
    
    String[] getLongPositionSymbols();

    String[] getShortPositionSymbols();
}
