package functionsalgo.samplestrat;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Backtester;
import functionsalgo.shared.Statistics.Results;

public class RunBacktest {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws StandardJavaException, ExchangeException {
        Results results = runBacktest(new BacktestConfiguration(), args);

        logger.info(results.textStats);
        if (!results.fileStats.isEmpty()) {
            for (File file : results.fileStats) {
                logger.info("More stats in the file: {}", file.getName());
            }
        }
    }

    public static Results runBacktest(BacktestConfiguration conf, String[] args)
            throws StandardJavaException, ExchangeException {
        Backtester backtester = new Backtester(conf, args);
        return backtester.run();
    }

}
