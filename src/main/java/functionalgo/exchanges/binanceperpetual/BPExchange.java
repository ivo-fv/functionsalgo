package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BPExchange extends Exchange {
    
    @Override
    BPExchangeAccountInfo getAccountInfo(long timestamp);
    
    boolean marketOpen(String positionId, String symbol, boolean isLong, double symbolQty);
    
    boolean marketClose(String positionId, double qtyToClose);
    
    void batchMarketOpen(String positionId, String symbol, boolean isLong, double symbolQty);
    
    void batchMarketClose(String positionId, double qtyToClose);
    
    /**
     * It is necessary to call getAccountInfo before every execution of this method.
     * This method may not execute every batched order.
     * The BPExchangeAccountInfo instance returned by executeBatchedOrders will contain the state of the account's
     * positions.
     * After executeBatchedOrders returns it's important to check if the positions opened by the executed orders
     * are as expected.
     * If after this method executes the state of the BPExchangeAccountInfo instance isn't as expected, it's up to
     * the caller to correct it batching the necessary corrective orders and calling executeBatchedOrders again.
     * 
     * @return a BPExchangeAccountInfo instance if one or more orders were executed, null if none were executed
     */
    BPExchangeAccountInfo executeBatchedOrders();
}
