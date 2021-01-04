package functionalgo;

/**
 * If implementing a simulated exchange, first update the account's position's PnL, margin balance, wallet
 * balance, calculate funding fees, perform margin checks...
 * 
 * An Exchange has methods to get an exchange/broker account information and to place trades. Exactly what
 * they are is up to the specific exchange implementation to decide.
 */
public interface Exchange {
    
    /**
     * Gets the account information at the exchange.
     * 
     * @param timestamp
     *            current unix timestamp in miliseconds
     */
    ExchangeAccountInfo getAccountInfo(long timestamp);
    
}
