package functionsalgo.shared;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statistics {

    public class Results {
        public String textStats;
        public List<File> fileStats;
    }

    protected Map<Long, Double> balances = new HashMap<>();

    public Results calculateStatistics() {
        // TODO max min drawdown, plot file
        return null;
    }

    public void addBalance(long timestamp, double balance) {
        balances.put(timestamp, balance);
    }
}
