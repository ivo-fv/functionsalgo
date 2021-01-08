package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BPExchange extends Exchange {
    
    @Override
    BPExchangeAccountInfo getAccountInfo(long timestamp);
    
    boolean isRandomFailingOrdersEnabled();
    
    boolean marketOpen(String positionId, String symbol, boolean isLong, double symbolQty);
    
    boolean marketClose(String positionId, double qtyToClose);
}
