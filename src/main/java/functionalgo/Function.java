package functionalgo;

import functionalgo.exceptions.ExchangeException;
import functionalgo.exceptions.StandardJavaException;

public class Function {
    
    private static final boolean IS_LIVE = false;
    private static final String STRATEGY_NAME = "functionalgo.perpetual.strategies.FLSStrategy";
    
    public static void main(String[] args) throws StandardJavaException, ExchangeException {
        
        if (IS_LIVE) {
            Strategy.setupStrategy(STRATEGY_NAME, true).execute(0);
            return;
        }
    }
}
