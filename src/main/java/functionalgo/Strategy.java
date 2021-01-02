package functionalgo;

import java.lang.reflect.InvocationTargetException;

import functionalgo.exceptions.StandardJavaException;

public abstract class Strategy {
    
    protected final boolean isLive;
    protected Exchange exchange;
    protected DataProvider dataProvider;
    protected ExchangeTradeExecutor exchangeTradeExecutor;
    
    public static Strategy setupStrategy(String stratClassName, boolean isLive, String exchangeName, String dataProviderName,
            String tradeExecutorName) throws StandardJavaException {
        
        try {
            return (Strategy) Class.forName(stratClassName).getConstructor().newInstance(false, exchangeName, dataProviderName,
                    tradeExecutorName);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new StandardJavaException(e);
        }
    }
    
    public Strategy(boolean isLive, String exchangeName, String dataProviderName, String tradeExecutorName)
            throws StandardJavaException {
        
        this.isLive = isLive;
        try {
            exchange = (Exchange) Class.forName(exchangeName).getConstructor().newInstance();
            dataProvider = (DataProvider) Class.forName(dataProviderName).getConstructor().newInstance();
            exchangeTradeExecutor = (ExchangeTradeExecutor) Class.forName(tradeExecutorName).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new StandardJavaException(e);
        }
    }
    
    /**
     * TODO (redo doc) First decide which exchange to get an ExchangeAccountState from, grab the needed data
     * (candles...), then decide
     * what actions to take, then execute those actions.
     * Execute the actions by sending them to the Exchange, and Log them.
     * 
     * @param timestamp
     *            unix timestamp in seconds of when this should execute, ignored when live trading (TODO???)
     */
    public abstract void execute(long timestamp);
    
}
