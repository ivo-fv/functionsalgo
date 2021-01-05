package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BPExchange extends Exchange {
    
    @Override
    BPExchangeAccountInfo getAccountInfo(long timestamp);
    
    boolean isRandomFailingOrdersEnabled();
    
    String[] getLongPositionSymbols();

    String[] getShortPositionSymbols();

    void marketOpenLong(String symbol, double symbolQty);

    void marketOpenShort(String symbol, double symbolQty);

    void marketCloseLong(String symbol);

    void marketCloseShort(String symbol);

    void marketReduceLong(String symbol, double symbolQty);

    void marketReduceShort(String symbol, double symbolQty);
}
