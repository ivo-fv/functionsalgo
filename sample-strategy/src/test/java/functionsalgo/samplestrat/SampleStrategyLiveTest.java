package functionsalgo.samplestrat;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import functionsalgo.exceptions.ExchangeException;
import functionsalgo.shared.Strategy;
import functionsalgo.shared.Statistics;

public class SampleStrategyLiveTest {
    static Strategy sampleStrat;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        sampleStrat = SampleStrategy.getLiveStrategy();
    }

    @Test
    public final void testExecute() throws ExchangeException {
        // TODO reflection to use a mock StrategyDecision
        /*
         * SampleStrategyStatistics stats = (SampleStrategyStatistics)
         * sampleStrat.execute(System.currentTimeMillis());
         */
        // TODO stats.toString().equals("expected")
    }

}
