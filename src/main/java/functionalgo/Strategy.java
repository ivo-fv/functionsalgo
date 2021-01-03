package functionalgo;

import java.lang.reflect.InvocationTargetException;

import functionalgo.exceptions.StandardJavaException;

public abstract class Strategy {
    
    protected final boolean isLive;
    
    public static Strategy setupStrategy(String stratClassName, boolean isLive) throws StandardJavaException {
        
        try {
            return (Strategy) Class.forName(stratClassName).getConstructor().newInstance(false);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new StandardJavaException(e);
        }
    }
    
    protected Strategy(boolean isLive){
        this.isLive = isLive;
    }
    
    /**
     * TODO (redo doc) First decide which exchange to get information from, grab the needed data
     * (candles...), then decide what actions to take, then execute those actions.
     * Execute the actions by sending them to the Exchange, and Log them.
     * 
     * @param timestamp
     *            current unix timestamp in miliseconds
     */
    public abstract void execute(long timestamp);
    
}
