package functionsalgo.binanceperpetual.exchange;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.datapoints.Interval;

public class SimExchangeTest {

    static SimExchange sim;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ClassLoader ldr = SimExchangeTest.class.getClassLoader();
        HistoricKlines bpHistoricKlines = HistoricKlines
                .loadKlines(new File(ldr.getResource("test_sim_binance_perp_klines_5m").getFile()));
        HistoricFundingRates bpHistoricFundingRates = HistoricFundingRates
                .loadFundingRates(new File(ldr.getResource("test_sim_binance_perp_fund_rates").getFile()));
        SlippageModel slippageModel = SlippageModel
                .loadSlippageModel(new File(ldr.getResource("test_sim_slippage_model").getFile()));
        sim = new SimExchange(1000, (short) 20, Interval._5m, bpHistoricKlines, bpHistoricFundingRates, slippageModel);
    }

    @Test
    public final void testAll() {
        // fail("Not yet implemented"); // TODO
    }

}
