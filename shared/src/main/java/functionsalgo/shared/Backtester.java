package functionsalgo.shared;

import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;

public class Backtester {

    private BacktestConfiguration config;
    private Strategy algo;

    /**
     * Initializes a BacktestConfiguration as specified by the passed command line
     * args.
     * <p>
     * The first arg must be a relative path to a BacktestConfiguration
     * configuration file (TODO provide a default file in resources). This file must
     * contain the information needed to initialize a BacktestConfiguration. If the
     * path contains spaces it must be enclosed within "quotes".
     * <p>
     * There are two flags that can be used: -gen or -genforce. If -gen then the
     * necessary support files needed to run a backtest will be generated. If
     * -genforce they will always be generated even if they already exist.
     * 
     * @param config the BacktestConfiguration instance to be initialized
     * @param args   the CLI args: configfile [-gen][-genforce]
     * @throws StandardJavaException
     */
    public Backtester(BacktestConfiguration config, String[] args) throws StandardJavaException {
        if (args == null || args.length == 0 || args[0].equals("")) {
            throw new IllegalArgumentException("mandatory CLI arguments: configfile [-gen][-genforce]");
        }
        this.config = config;
        String configFile = args[0];
        boolean gen = args.length > 1 && (args[1].equals("-gen") || args[1].equals("-genforce")) ? true : false;
        boolean force = args.length > 1 && (args[1].equals("-genforce")) ? true : false;

        config.loadConfiguration(configFile, gen, force);
    }

    public Statistics.Results run() throws StandardJavaException, ExchangeException {
        algo = config.getStrategy();
        Statistics stats = null;

        for (long t = config.getBacktestStartTime().getTime(); t <= config.getBacktestEndTime().getTime(); t += config
                .getBacktestInterval().toMilliseconds()) {
            stats = algo.execute(t);
        }
        return stats.calculateStatistics();
    }

}
