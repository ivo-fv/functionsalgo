package functionsalgo.shared;

import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Timestamp;
import functionsalgo.exceptions.StandardJavaException;

public interface BacktestConfiguration {

    void loadConfiguration(String configFile, boolean gen, boolean forceGen) throws StandardJavaException;

    Strategy getStrategy() throws StandardJavaException;

    Timestamp getBacktestStartTime();

    Timestamp getBacktestEndTime();

    Interval getBacktestInterval();
}
