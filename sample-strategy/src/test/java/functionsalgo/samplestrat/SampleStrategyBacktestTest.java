package functionsalgo.samplestrat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Utils;
import functionsalgo.shared.Statistics.Results;

public class SampleStrategyBacktestTest {

    private static final Logger logger = LogManager.getLogger();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder(new File("."));

    @Test
    public final void testRunBacktestStrategy() throws IOException, StandardJavaException, ExchangeException {

        final String expectedBacktestResultStats = "\nFinal balance: 668.4376462450928\n"
                + "Max balance: 1024.326832544406\n" + "Min balance: 612.7214345444348\n"
                + "Max drawdown %: 40.183014339041776\n" + "Final wallet balance: 756.2357555303327\n"
                + "Max wallet balance: 1024.3052256579413\n" + "Min wallet balance: 756.2357555303327\n"
                + "Max wallet drawdown %: 26.17085839383664";

        HistoricKlines klines = (HistoricKlines) Utils.loadObjectFileOrResource("sample_backtest_klines");
        String fratesLoc = temporaryFolder.getRoot().getPath() + File.separator + "frates";
        SlippageModel slipp = (SlippageModel) Utils.loadObjectFileOrResource("sample_backtest_slippage_model");
        BacktestConfiguration conf = new BacktestConfiguration();
        conf.withBPKlines(klines).withBPFundingRatesLocation(fratesLoc).withBPSlippageModel(slipp);

        try {
            RunBacktest.runBacktest(conf, new String[] { "sample_backtest_config.properties" });
            fail("when passing no gen or genforce flag and no usable files were pre generated it should throw");
        } catch (Exception e) {
            // should throw, test OK
        }

        Results results = RunBacktest.runBacktest(conf,
                new String[] { "sample_backtest_config.properties", "-genforce" });
        logger.info(results.textStats);

        if (!expectedBacktestResultStats.equals(results.textStats)) {
            logger.error("pulled funding rates likely changed, trying again with a default funding rates file");
            conf.withBPFundingRates(
                    (HistoricFundingRates) Utils.loadObjectFileOrResource("sample_backtest_funding_rates"));
            results = RunBacktest.runBacktest(conf, new String[] { "sample_backtest_config.properties", "-genforce" });
            logger.info(results.textStats);
            assertTrue("results not as expected", expectedBacktestResultStats.equals(results.textStats));
        }

        results = RunBacktest.runBacktest(conf, new String[] { "sample_backtest_config.properties", "-gen" });
        logger.info(results.textStats);
        assertTrue("results not as expected", expectedBacktestResultStats.equals(results.textStats));

        results = RunBacktest.runBacktest(conf, new String[] { "sample_backtest_config.properties" });
        logger.info(results.textStats);
        assertTrue("results not as expected", expectedBacktestResultStats.equals(results.textStats));
    }
}
