package functionsalgo.binanceperpetual.exchange;

import functionsalgo.binanceperpetual.exchange.exceptions.OrderExecutionException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolNotTradingException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolQuantityTooLow;
import functionsalgo.exceptions.ExchangeException;

public interface Exchange {

    AccountInfo getAccountInfo(long timestamp) throws ExchangeException;

    void setHedgeMode() throws ExchangeException;;

    void setLeverage(String symbol, int leverage) throws ExchangeException;

    void setCrossMargin(String symbol) throws ExchangeException;

    void addBatchMarketOpen(int orderId, String symbol, boolean isLong, double symbolQty)
            throws SymbolQuantityTooLow, SymbolNotTradingException;

    void addBatchMarketClose(int orderId, String symbol, boolean isLong, double qtyToClose)
            throws SymbolQuantityTooLow, SymbolNotTradingException;

    AccountInfo executeBatchedMarketOpenOrders() throws OrderExecutionException;

    AccountInfo executeBatchedMarketCloseOrders() throws OrderExecutionException;
}
