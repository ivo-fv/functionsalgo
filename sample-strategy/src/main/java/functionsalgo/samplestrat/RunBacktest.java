package functionsalgo.samplestrat;

import functionsalgo.shared.Backtester;

public class RunBacktest {

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        BacktestConfiguration config = new BacktestConfiguration();
        Backtester backtester = new Backtester(config, args);
        Backtester.BacktestResult results = backtester.run();
    }

}
