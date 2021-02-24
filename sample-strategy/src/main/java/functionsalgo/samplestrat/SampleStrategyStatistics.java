package functionsalgo.samplestrat;

import java.util.HashMap;
import java.util.Map;

import functionsalgo.shared.Statistics;

public class SampleStrategyStatistics extends Statistics {

    private Map<Long, Double> walletBalances = new HashMap<>();

    @Override
    public Results calculateStatistics() {
        Results results = super.calculateStatistics();

        double maxBalance = 0;
        double minBalance = Double.MAX_VALUE;
        double maxDrawdown = 0;

        for (double balance : walletBalances.values()) {
            maxBalance = Math.max(maxBalance, balance);
            minBalance = Math.min(minBalance, balance);
            maxDrawdown = Math.max(maxDrawdown, 1 - (balance / maxBalance));
        }

        results.textStats += "\nFinal wallet balance: " + walletBalances.get(lastTimestamp) + "\nMax wallet balance: "
                + maxBalance + "\nMin wallet balance: " + minBalance + "\nMax wallet drawdown %: " + maxDrawdown * 100;

        // TODO svg chart of walletBalances

        return results;
    }

    public void addWalletBalance(long timestamp, double balance) {
        walletBalances.put(timestamp, balance);
        lastTimestamp = timestamp;
    }
}
