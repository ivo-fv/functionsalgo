package functionalgo.binanceperpetual.exchange;

import functionalgo.exceptions.ExchangeException;

public interface BPAccount {
    
    long getTimestamp();
    
    double getWalletBalance();
    
    int getLeverage(String symbol);
    
    double getTakerFee();
    
    double getMarginBalance();
    
    double getQuantity(String symbol, boolean isLong);
    
    double getAverageOpenPrice(String symbol, boolean isLong);
    
    long getNextFundingTime(String symbol);
    
    double getFundingRate(String symbol);
    
    double getWorstMarginBalance();
    
    double getMarkPrice(String symbol);
    
    /**
     * @param orderId
     *            id provided when batching an order
     * @return the error, or null if no error
     */
    ExchangeException getOrderError(String orderId);
    
    double getOrderQuantity(String orderId);
    
    boolean isBalancesDesynch();

    boolean isPositionsDesynch();

    boolean isSymbolIsolated(String symbol);

    boolean isHedgeMode();
}
