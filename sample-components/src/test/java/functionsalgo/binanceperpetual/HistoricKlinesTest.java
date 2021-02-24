package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import functionsalgo.datapoints.Timestamp;
import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.StandardJavaException;

public class HistoricKlinesTest {

    @Test
    public final void testHistoricKlinesAll() throws StandardJavaException {

        HistoricKlines.DATA_DIR = "../.genresources/test_data";
        HistoricKlines.JSON_DATA_DIR = HistoricKlines.DATA_DIR + "/binance_perp_json_data";

        ArrayList<String> symbols = new ArrayList<>();
        symbols.add("BTCUSDT");
        symbols.add("ETHUSDT");

        Timestamp start = new Timestamp(1612370647356L, Interval._5m);
        Timestamp end = new Timestamp(1612470647356L, Interval._5m);
        HistoricKlines klines = HistoricKlines.pullKlines(symbols, Interval._5m, start, end);
        Interval interval = klines.getInterval();
        assertTrue("invalid klines object file - getInterval", klines.getInterval() == Interval._5m);
        assertTrue("invalid klines object file - getKlines", klines
                .getKlines("ETHUSDT", new Timestamp(1612390647356L, interval), new Timestamp(1612395648222L, interval))
                .get(0).getOpen() >= 1000);
        assertTrue("invalid klines object file - getKline",
                klines.getKline("ETHUSDT", new Timestamp(1612390647356L, interval)).getOpen() >= 1000);
    }
}
