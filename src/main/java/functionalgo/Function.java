package functionalgo;

import functionalgo.exceptions.StandardJavaException;

public class Function {
    
    private static final boolean IS_LIVE = false;
    private static final String STRATEGY_NAME = "functionalgo.perpetual.strategies.FLSStrategy";
    
    private Strategy strategy;
    
    public static void main(String[] args) throws StandardJavaException {
        
        if (IS_LIVE) {
            new Function(Strategy.setupStrategy(STRATEGY_NAME, true)).run(0);
            return;
        } else {
            
            // TODO set times and interval for a backtest run
            
            long initialTime = 0;
            long endTime = 0;
            long interval = 0; // usar UPDATE_INTERVAL_MILLIS da BPSimExchange
            
            Function function = new Function(Strategy.setupStrategy(STRATEGY_NAME, false));
            
            for (long t = initialTime; t < endTime; t += interval) {
                
                function.run(t);
            }
        }
    }
    
    public Function(Strategy strategy) {
        
        this.strategy = strategy;
    }
    
    /**
     * @param timestamp
     *            current unix timestamp in miliseconds
     */
    public void run(long timestamp) {
        
        strategy.execute(timestamp);
    }
}
