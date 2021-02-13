package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.StandardJavaException;

public class HistoricKlinesExternalTest {

    @Test
    public final void testHistoricKlinesAll() throws StandardJavaException {

        HistoricKlines.DATA_DIR = "../.genresources/test_data";
        HistoricKlines.JSON_DATA_DIR = HistoricKlines.DATA_DIR + "/binance_perp_json_data";

        ArrayList<String> symbols = new ArrayList<>();
        symbols.add("BTCUSDT");
        symbols.add("ETHUSDT");

        HistoricKlines klines = HistoricKlines.pullKlines(symbols, Interval._5m, 1612370647356L, 1612470647356L);

        assertTrue("invalid klines object file - getInterval", klines.getInterval() == Interval._5m);
        assertTrue("invalid klines object file - getKlines",
                klines.getKlines("ETHUSDT", 1612390647356L, 1612395648222L).get(0).getOpen() >= 1000);
        assertTrue("invalid klines object file - getKline",
                klines.getKline("ETHUSDT", 1612390647356L).getOpen() >= 1000);
    }
}
