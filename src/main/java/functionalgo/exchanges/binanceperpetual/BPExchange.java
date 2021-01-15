package functionalgo.exchanges.binanceperpetual;

import functionalgo.exceptions.ExchangeException;

public interface BPExchange {
    
    BPAccount getAccountInfo(long timestamp) throws ExchangeException;
    
    void setHedgeMode() throws ExchangeException;;
    
    void setLeverage(String symbol, int leverage) throws ExchangeException;
    
    void setCrossMargin(String symbol) throws ExchangeException;
    
    void batchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty) throws ExchangeException;
    
    void batchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose) throws ExchangeException;
    
    /**
     * It is recommended to call getAccountInfo() at least once for every interval the strategy monitors before
     * calling executeBatchedOrders() an arbitrary amount of times.
     * This method may fail to execute every batched order. Every batch order will be consumed even if it didn't
     * get
     * executed. It will execute in the order the orders were batched.
     * The BPAccount instance returned by executeBatchedOrders is modified from the one returned by getAccountInfo
     * so they are the same instance. It will contain the state of the account's
     * positions and , if execution errors for a specific position occur, these errors (BPAccount.getOrderError).
     * If there's not enough margin to execute the orders, only close orders will be executed.
     * After executeBatchedOrders returns it's important to check if the positions opened by the executed orders
     * are as expected.
     * If after this method executes the state of the BPAccount instance isn't as expected, it's up to
     * the caller to correct it batching the necessary corrective orders and calling executeBatchedOrders again.
     * 
     * @return a BPAccount instance if one or more orders were executed, null if none were executed
     * @throws ExchangeException
     */
    BPAccount executeBatchedOrders() throws ExchangeException;
}
