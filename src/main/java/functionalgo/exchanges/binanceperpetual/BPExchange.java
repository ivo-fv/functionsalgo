package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BPExchange extends Exchange {
    
    @Override
    BPExchangeAccountInfo getAccountInfo(long timestamp);
    
    boolean isRandomFailingOrdersEnabled();
    
    void marketOpen(String positionId, String symbol, boolean isLong, double symbolQty);
    
    void marketClose(String positionId, double qtyToClose);
}
