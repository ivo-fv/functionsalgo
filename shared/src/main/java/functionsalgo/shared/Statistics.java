package functionsalgo.shared;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statistics {

    public class Results {
        public String textStats;
        public List<File> fileStats = new ArrayList<>();
    }

    protected Map<Long, Double> balances = new HashMap<>();
    protected long lastTimestamp;

    public Results calculateStatistics() {

        double maxBalance = 0;
        double minBalance = Double.MAX_VALUE;
        double maxDrawdown = 0;

        for (double balance : balances.values()) {
            maxBalance = Math.max(maxBalance, balance);
            minBalance = Math.min(minBalance, balance);
            maxDrawdown = Math.max(maxDrawdown, 1 - (balance / maxBalance));
        }

        Results results = new Results();
        results.textStats = "\nFinal balance: " + balances.get(lastTimestamp) + "\nMax balance: " + maxBalance
                + "\nMin balance: " + minBalance + "\nMax drawdown %: " + maxDrawdown * 100;

        // TODO svg chart of balances

        return results;
    }

    public void addBalance(long timestamp, double balance) {
        balances.put(timestamp, balance);
        lastTimestamp = timestamp;
    }
}
