package functionalgo;


public interface Strategy {

    /**
     * Based on the AccountState (positions, margin balance...) and the current state of a Strategy execute the strategy.
     * First grab the needed data (candles...), then decide what actions to take, then execute those actions.
     * Execute the actions by sending them to the Exchange, and Log them.
     * @param state
     */
    void execute(AccountState state);
    
}
