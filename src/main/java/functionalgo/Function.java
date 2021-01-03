package functionalgo;

import functionalgo.exceptions.StandardJavaException;

public class Function {
    
    private static final boolean IS_LIVE = false;
    private static final String STRATEGY_NAME = "functionalgo.perpetual.strategies.FLSStrategy";
    private static final String LIVE_EXCHANGE_NAME = "LiveExchange";
    private static final String LIVE_DATA_PROVIDER_NAME = "LiveDataProvider";
    private static final String LIVE_TRADE_EXECUTOR_NAME = "LiveTradeExecutor";
    private static final String SIM_EXCHANGE_NAME = "functionalgo.perpetual.exchanges.simbinance.SimBinancePerpetual";
    private static final String SIM_DATA_PROVIDER_NAME = "functionalgo.dataproviders.BacktestBinancePerpetualData";
    private static final String SIM_TRADE_EXECUTOR_NAME = "functionalgo.perpetual.exchanges.simbinance.SimBinancePerpetualTradeExecutor";
    
    private Strategy strategy;
    
    public static void main(String[] args) throws StandardJavaException {
        
        if (IS_LIVE) {
            new Function(Strategy.setupStrategy(STRATEGY_NAME, true, LIVE_EXCHANGE_NAME, LIVE_DATA_PROVIDER_NAME,
                    LIVE_TRADE_EXECUTOR_NAME)).run(0);
            return;
        } else {
            
            // TODO set times for a backtest run, get the interval from candles (klines) from an Exchange
            
            long initialTime = 0;
            long endTime = 0;
            long interval = 0;
            
            Function function = new Function(Strategy.setupStrategy(STRATEGY_NAME, false, SIM_EXCHANGE_NAME,
                    SIM_DATA_PROVIDER_NAME, SIM_TRADE_EXECUTOR_NAME));
            
            for (long t = initialTime; t < endTime; t += interval) {
                
                function.run(t);
            }
        }
    }
    
    public Function(Strategy strategy) {
        
        this.strategy = strategy;
    }
    
    /**
     * //TODO
     * 
     * @param timestamp
     *            current unix timestamp in miliseconds
     */
    public void run(long timestamp) {
        
        strategy.execute(timestamp);
    }
}
