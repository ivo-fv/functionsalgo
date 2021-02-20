package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import functionsalgo.datapoints.Timestamp;
import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.StandardJavaException;

public class HistoricFundingRatesTest {

    @Test
    public final void testHistoricFundingRateAll() throws StandardJavaException {

        HistoricFundingRates.DATA_DIR = "../.genresources/test_data";
        HistoricFundingRates.FUND_RATES_FILE = HistoricFundingRates.DATA_DIR + "/binance_perp_fund_rates";
        HistoricFundingRates.JSON_DATA_FOLDER = HistoricFundingRates.DATA_DIR + "/binance_perp_json_data/fund_rates";

        ArrayList<String> symbols = new ArrayList<>();
        symbols.add("BTCUSDT");
        symbols.add("ETHUSDT");

        HistoricFundingRates fundingRates = HistoricFundingRates.pullFundingRates(symbols, 1611646639946L,
                1612470647356L);

        assertTrue("invalid fundingRates object file - getFundingIntervalMillis",
                fundingRates.getFundingInterval().toMilliseconds() == Interval._8h.toMilliseconds());
        assertTrue("invalid fundingRates object file - getFundingRates",
                fundingRates.getFundingRates("ETHUSDT", new Timestamp(1611846639946L, Interval._8h),
                        new Timestamp(1611946644625L, Interval._8h)).get(0).getFundingRate() > -10);
        assertTrue("invalid fundingRates object file - getFundingRates", fundingRates
                .getFundingRate("ETHUSDT", new Timestamp(1611846639946L, Interval._8h)).getFundingRate() > -10);
    }
}