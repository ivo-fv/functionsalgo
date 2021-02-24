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
        Timestamp start = new Timestamp(1611646639946L, Interval._8h);
        Timestamp end = new Timestamp(1612470647356L, Interval._8h);
        HistoricFundingRates fundingRates = HistoricFundingRates.pullFundingRates(symbols, start, end);
        Interval interval = fundingRates.getFundingInterval();
        assertTrue("invalid fundingRates object file - getFundingIntervalMillis",
                fundingRates.getFundingInterval().toMilliseconds() == interval.toMilliseconds());
        assertTrue("invalid fundingRates object file - getFundingRates",
                fundingRates.getFundingRates("ETHUSDT", new Timestamp(1611846639946L, interval),
                        new Timestamp(1611946644625L, interval)).get(0).getFundingRate() > -10);
        assertTrue("invalid fundingRates object file - getFundingRates",
                fundingRates.getFundingRate("ETHUSDT", new Timestamp(1611846639946L, interval)).getFundingRate() > -10);
    }
}