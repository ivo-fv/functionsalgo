package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.StandardJavaException;

public class HistoricFundingRatesExternalTest {

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
                fundingRates.getFundingIntervalMillis() == Interval._8h.toMilliseconds());
        assertTrue("invalid fundingRates object file - getFundingRates",
                fundingRates.getFundingRates("ETHUSDT", 1611846639946L, 1611946639946L).get(0).getFundingRate() > -10);
    }
}
