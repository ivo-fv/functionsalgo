package functionalgo;

import java.lang.reflect.InvocationTargetException;

import functionalgo.exceptions.StandardJavaException;

public abstract class Strategy {
    
    protected final boolean isLive;
    
    public static Strategy setupStrategy(String stratClassName, boolean isLive) throws StandardJavaException {
        
        try {
            return (Strategy) Class.forName(stratClassName).getConstructor().newInstance(isLive);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new StandardJavaException(e);
        }
    }
    
    protected Strategy(boolean isLive) {
        
        this.isLive = isLive;
    }
    
    /**
     * Runs the strategy logic for the given timestamp.
     * 
     * @param timestamp
     *            current unix timestamp in miliseconds
     */
    public abstract void execute(long timestamp);
    
}
