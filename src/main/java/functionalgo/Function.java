package functionalgo;

import java.lang.reflect.InvocationTargetException;

public class Function {
    
    private static final String EXCHANGE_NAME = null;
    private static final String STRATEGY_NAME = null;
    
    static Exchange exchange = getExchange();
    
    private static Exchange getExchange() {
        
        try {
            return (Exchange) Class.forName(EXCHANGE_NAME).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            
            return null;
        }
    }
    
    public static void run(long t) {
        
        AccountState state = exchange.getAccountState(t);
        
        Strategy lsStrat = StrategyState.getStrategy(STRATEGY_NAME);
        lsStrat.execute(state);
        
        // TODO Auto-generated method stub
        
    }
    
}
