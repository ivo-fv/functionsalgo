package functionalgo;

public interface Exchange {
    
    /**
     * If implementing a simulated exchange, first update the account's position's PnL, margin balance, wallet
     * balance, calculate funding fees, perform margin checks...
     * For a live exchange implementation, grab the needed information to construct an ExchangeAccountState.
     * 
     * @param timestamp
     *            requires a valid timestamp if backtesting
     * @return state of the trading account at the exchange
     */
    ExchangeAccountState getAccountState(long timestamp);
    
}
