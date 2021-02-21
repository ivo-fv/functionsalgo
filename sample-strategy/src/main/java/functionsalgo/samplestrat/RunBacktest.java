package functionsalgo.samplestrat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Backtester;
import functionsalgo.shared.Statistics.Results;

public class RunBacktest {

    private static final Logger logger = LogManager.getLogger();
    
    public static void main(String[] args) throws IOException, StandardJavaException {
        // TODO Auto-generated method stub

        BacktestConfiguration config = new BacktestConfiguration();
        Backtester backtester = new Backtester(config, args);
        Results results = backtester.run();
        
        logger.info(results.textStats);
    }

}
