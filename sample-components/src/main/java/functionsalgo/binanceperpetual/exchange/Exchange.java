package functionsalgo.binanceperpetual.exchange;

import functionsalgo.exceptions.ExchangeException;

public interface Exchange {

    AccountInfo getAccountInfo(long timestamp) throws ExchangeException;

    void setHedgeMode() throws ExchangeException;;

    void setLeverage(String symbol, int leverage) throws ExchangeException;

    void setCrossMargin(String symbol) throws ExchangeException;

    void addBatchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty) throws ExchangeException;

    void addBatchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose) throws ExchangeException;

    AccountInfo executeBatchedMarketOpenOrders() throws ExchangeException;

    AccountInfo executeBatchedMarketCloseOrders() throws ExchangeException;
}
