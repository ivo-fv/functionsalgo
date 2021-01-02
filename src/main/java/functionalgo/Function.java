package functionalgo;

import java.lang.reflect.InvocationTargetException;

public class Function {
    
    private static final boolean IS_LIVE = false;
    private static final String STRATEGY_NAME = "somestrat";
    
    private Strategy strategy;
    
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        
        if (IS_LIVE) {
            new Function(setupLiveStrategy(STRATEGY_NAME)).run(0);
            
        } else {
            
            // TODO set times for a backtest run, get the interval from candles (klines) from an Exchange
            
            long initialTime = 0;
            long endTime = 0;
            long interval = 0;
            
            Function function = new Function(setupBacktestStrategy(STRATEGY_NAME));
            
            for (long t = initialTime; t < endTime; t += interval) {
                
                function.run(t);
            }
        }
    }
    
    public static Strategy setupLiveStrategy(String name) {
        
        // TODO return a stateful strategy from a database, if no strategy available setup one
        return null;
    }
    
    public static Strategy setupBacktestStrategy(String name)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException, ClassNotFoundException {
        
        return (Strategy) Class.forName(name).getConstructor().newInstance();
    }
    
    public Function(Strategy strategy) {
        
        this.strategy = strategy;
    }
    
    /**
     * //TODO
     * 
     * @param timestamp
     *            unix timestamp in seconds of when this should execute
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws ClassNotFoundException
     */
    public void run(long timestamp) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        
        strategy.execute(timestamp);
    }
}
