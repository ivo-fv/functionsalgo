package functionsalgo.samplestrat;

import java.util.HashMap;
import java.util.Map;

import functionsalgo.shared.Statistics;

public class SampleStrategyStatistics extends Statistics {

    private Map<Long, Double> walletBalances = new HashMap<>();

    @Override
    public Results calculateStatistics() {
        // TODO max min drawdown, plot file, including new SampleStrat specific stuff
        super.calculateStatistics();// .....
        throw new RuntimeException("SampleStrategyStatistics - extension :" + walletBalances.get(1234567L));
        // return null;
    }

    public void addWalletBalance(long timestamp, double balance) {
        walletBalances.put(timestamp, balance);
    }
}
