package functionsalgo.samplestrat;

import java.util.HashMap;
import java.util.Map;

import functionsalgo.shared.Statistics;

public class SampleStrategyStatistics extends Statistics {

    private Map<Long, Double> marginBalances = new HashMap<>();

    @Override
    public Results calculateStatistics() {
        // TODO max min drawdown, plot file, including new SampleStrat specific stuff
        return null;
    }

    public void addWalletBalance(long timestamp, double balance) {
        marginBalances.put(timestamp, balance);
    }
}
