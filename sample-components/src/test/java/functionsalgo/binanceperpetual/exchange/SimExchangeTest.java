package functionsalgo.binanceperpetual.exchange;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.datapoints.Timestamp;
import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.ExchangeException;

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
        sim = new SimExchange(10000, (short) 20, Interval._5m, bpHistoricKlines, bpHistoricFundingRates, slippageModel);
    }

    @Test
    public final void testAll() throws ExchangeException {
        AccountInfo accInfo = sim.getAccountInfo(1604966761000L);
        assertTrue("accInfo incorrect initialization",
                accInfo.getMarginBalance() == 10000 && accInfo.getTotalInitialMargin() == 0);

        accInfo = sim.getAccountInfo(1605147243000L);
        assertTrue("bad timestamp", accInfo.getTimestampMillis() == 1605147000000L);

        sim.addBatchMarketOpen("1", "ETHUSDT", true, 0.4);
        sim.addBatchMarketOpen("2", "BTCUSDT", false, 0.01);
        sim.addBatchMarketOpen("3", "BTCUSDT", true, 0.001);
        sim.executeBatchedMarketOpenOrders();

        for (Timestamp t = new Timestamp(1605147953000L, Interval._5m); t.getTime() <= 1605198953000L; t
                .inc()) {
            sim.getAccountInfo(t.getTime());
        }
        accInfo = sim.getAccountInfo(1605408953000L);

        sim.addBatchMarketClose("10", "ETHUSDT", true, 0.05);
        sim.addBatchMarketClose("20", "BTCUSDT", true, 10);
        sim.addBatchMarketClose("20", "NONEXISTENTSYMBOL", true, 2);
        accInfo = sim.executeBatchedMarketCloseOrders();

        assertTrue("bad position sizes", Math.abs(accInfo.getQuantity("ETHUSDT", true) - 0.35) < 0.0001
                && accInfo.getQuantity("BTCUSDT", false) == 0.01 && accInfo.getQuantity("BTCUSDT", true) == 0);
        assertTrue("must be an error",
                accInfo.getOrderErrors().get(0).status == OrderError.OrderStatus.NO_SUCH_POSITION_FAILED);

        accInfo = sim.getAccountInfo(1612404843000L);

        assertTrue("bad wallet balance", Math.abs(accInfo.getWalletBalance() - 10000.23) < 0.01);

        sim.addBatchMarketClose("100", "ETHUSDT", true, 1);
        sim.addBatchMarketClose("200", "BTCUSDT", false, 1);
        accInfo = sim.executeBatchedMarketCloseOrders();

        assertTrue("bad final balances", Math.abs(accInfo.getWalletBalance() - accInfo.getMarginBalance()) < 0.01
                && Math.abs(accInfo.getMarginBalance() - 10205.528) < 0.01);
    }

}
