package functionsalgo.samplestrat;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Backtester;
import functionsalgo.shared.Statistics.Results;

public class RunBacktest {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws IOException, StandardJavaException {

        BacktestConfiguration config = new BacktestConfiguration();
        Backtester backtester = new Backtester(config, args);
        Results results = backtester.run();

        logger.info(results.textStats);
        if (!results.fileStats.isEmpty()) {
            for (File file : results.fileStats) {
                logger.info("More stats in the file: {}", file.getName());
            }
        }
    }

}
