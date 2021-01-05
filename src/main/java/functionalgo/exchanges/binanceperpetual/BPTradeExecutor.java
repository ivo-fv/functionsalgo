package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeTradeExecutor;

public interface BPTradeExecutor extends ExchangeTradeExecutor {
    
    void marketOpenLong(String symbol, double symbolQty);
    
    void marketOpenShort(String symbol, double symbolQty);
    
    void marketCloseLong(String symbol);
    
    void marketCloseShort(String symbol);
    
    void marketReduceLong(String symbol, double symbolQty);
    
    void marketReduceShort(String symbol, double symbolQty);
}
