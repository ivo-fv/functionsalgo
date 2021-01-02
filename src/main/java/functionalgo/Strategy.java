package functionalgo;

public interface Strategy {
    
    /**
     * First decide which exchange to get an AccountState from, grab the needed data (candles...), then decide
     * what actions to take, then execute those actions.
     * Execute the actions by sending them to the Exchange, and Log them.
     * 
     * @param timestamp
     *            unix timestamp in seconds of when this should execute, ignored when live trading (TODO???)
     */
    void execute(long timestamp); // TODO maybe throw custom wrapper exception
    
}
