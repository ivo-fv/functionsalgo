package functionalgo.exchanges.binanceperpetual;

import functionalgo.Exchange;

public interface BPExchange extends Exchange {
    
    @Override
    BPExchangeAccountInfo getAccountInfo(long timestamp);
    
    boolean marketOpen(String symbol, boolean isLong, double symbolQty);
    
    boolean marketClose(String symbol, boolean isLong, double qtyToClose);
    
    void batchMarketOpen(String symbol, boolean isLong, double symbolQty);
    
    void batchMarketClose(String symbol, boolean isLong, double qtyToClose);
    
    /**
     * It is recommended to call getAccountInfo() at least once for every interval the strategy monitors before
     * calling executeBatchedOrders() an arbitrary amount of times.
     * This method may fail to execute every batched order. Every batch order will be consumed even if it didn't get
     * executed. It will execute in the order the orders were batched.
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
