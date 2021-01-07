package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeTradeExecutor;

public interface BPTradeExecutor extends ExchangeTradeExecutor {
    
    void marketOpen(String positionId, String symbol, boolean isLong, double symbolQty);

    void marketClose(String positionId, double qtyToClose);
}
