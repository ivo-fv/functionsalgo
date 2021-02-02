package functionsalgo.binanceperpetual.dataprovider;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import functionsalgo.aws.DynamoDBBPDataProvider;
import functionsalgo.aws.DynamoDBCommon;
import functionsalgo.binanceperpetual.WrapperREST;
import functionsalgo.datapoints.FundingRate;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;

public class LiveDataProvider implements DataProvider {

    /**
     * Used for quick testing
     * 
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static void main(String[] args) throws ExchangeException, InvalidKeyException, NoSuchAlgorithmException {

        DataProviderDB database = new DynamoDBBPDataProvider(new DynamoDBCommon());

        LiveDataProvider dataProvider = new LiveDataProvider(database);

        try {
            List<Kline> klines = dataProvider.getExchangeKlines("ETHUSDT", Interval._5m, 1598346800000L,
                    1599562800000L);
            System.out.println(klines.get(0).getOpenTime());
        } catch (ExchangeException e) {
            System.out.println(e.toString() + " ; " + Arrays.toString(e.getStackTrace()));
            // e.printStackTrace();
        }
        try {
            List<FundingRate> frates = dataProvider.getExchangeFRates("ETHUSDT", 1599580799900L, 1599580800100L);
            System.out.println(frates.get(0).getFundingTime());
        } catch (ExchangeException e) {
            e.printStackTrace();
        }
    }

    private static final String HOST = WrapperREST.HOST_LIVE;
    private static final String ENDPOINT_KLINES = "/fapi/v1/klines";
    private static final int KLINES_MAX_NUM_PER_REQ = 1000;
    private static final String ENDPOINT_FRATES = "/fapi/v1/fundingRate";
    private static final int FRATES_MAX_NUM_PER_REQ = 500;
    private static final Interval FUNDING_INTERVAL = Interval._8h;
    private static final long FRATES_LEEWAY = 100;

    private static final Logger logger = LogManager.getLogger();

    private DataProviderDB database;
    private WrapperREST apiHandler;

    public LiveDataProvider(DataProviderDB database) throws ExchangeException {

        this.database = database;
        this.apiHandler = apiHandler;
    }

    @Override
    public List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws ExchangeException {

        List<FundingRate> dbFRates = getDBFundingRates(symbol, startTime, endTime);

        long expectedNumFRates = (((endTime / FUNDING_INTERVAL.toMilliseconds()) * FUNDING_INTERVAL.toMilliseconds())
                - ((startTime / FUNDING_INTERVAL.toMilliseconds()) * FUNDING_INTERVAL.toMilliseconds()))
                / FUNDING_INTERVAL.toMilliseconds() + 1;

        if (expectedNumFRates == dbFRates.size()) {
            return dbFRates;
        } else {
            long adjustedStartTime = (startTime / FUNDING_INTERVAL.toMilliseconds())
                    * FUNDING_INTERVAL.toMilliseconds();
            List<FundingRate> newFRates = getExchangeFRates(symbol, adjustedStartTime - FRATES_LEEWAY,
                    endTime + FRATES_LEEWAY);
            setDBFRates(symbol, adjustedStartTime, newFRates);
            return newFRates;
        }
    }

    private List<FundingRate> getExchangeFRates(String symbol, long startTime, long endTime) throws ExchangeException {

        if (startTime == endTime) {
            endTime++;
        }

        List<FundingRate> returnList = new ArrayList<>();

        long nextPageTimeAdd = FUNDING_INTERVAL.toMilliseconds() * FRATES_MAX_NUM_PER_REQ;

        try {
            while (startTime < endTime) {

                String req = "GET " + ENDPOINT_FRATES + "?symbol=" + symbol + "&startTime=" + startTime + "&endTime="
                        + endTime + "&limit=" + FRATES_MAX_NUM_PER_REQ + " HTTP/1.1\r\nConnection: close\r\nHost: "
                        + HOST + "\r\n\r\n";

                JsonElement fratesPage = null; // TODO implement necessary method in api handler, refactor to not use
                                               // JsonElement here
                JsonArray klines = fratesPage.getAsJsonArray();
                for (JsonElement elem : klines) {
                    JsonObject fr = elem.getAsJsonObject();
                    returnList.add(new FundingRate(fr.get("symbol").getAsString(), fr.get("fundingRate").getAsDouble(),
                            fr.get("fundingTime").getAsLong()));
                }

                startTime += nextPageTimeAdd;
            }
        } catch (Exception e) {
            if (e instanceof ExchangeException) {
                throw e;
            } else {
                throw new ExchangeException(ExchangeException.ErrorType.PARSING_PROBLEM,
                        "Problem parsing funding rates data", "LiveDataProvider::getExchangeFRates", e);
            }
        }

        return returnList;
    }

    private List<FundingRate> getDBFundingRates(String symbol, long startTime, long endTime) {

        List<FundingRate> rates = database.getFundingRates(symbol, startTime, endTime);

        if (rates.isEmpty()) {
            logger.warn("getDBFundingRates : Rates list empty");
        }

        return rates;
    }

    private void setDBFRates(String symbol, long startTime, List<FundingRate> newFRates) {

        database.setFundingRates(symbol, startTime, newFRates);
    }

    @Override
    public long getFundingInterval() {

        return FUNDING_INTERVAL.toMilliseconds();
    }

    @Override
    public List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime)
            throws ExchangeException {

        List<Kline> dbKlines = getDBKlines(symbol, interval, startTime, endTime);

        long expectedNumKlines = (((endTime / interval.toMilliseconds()) * interval.toMilliseconds())
                - ((startTime / interval.toMilliseconds()) * interval.toMilliseconds())) / interval.toMilliseconds()
                + 1;

        if (expectedNumKlines == dbKlines.size()) {
            return dbKlines;
        } else {
            long adjustedStartTime = ((startTime / interval.toMilliseconds()) * interval.toMilliseconds())
                    - interval.toMilliseconds();
            List<Kline> newKlines = getExchangeKlines(symbol, interval, adjustedStartTime, endTime);
            setDBKlines(symbol, interval, adjustedStartTime, newKlines);
            newKlines.remove(0);
            return newKlines;
        }
    }

    private List<Kline> getExchangeKlines(String symbol, Interval interval, long startTime, long endTime)
            throws ExchangeException {

        if (startTime == endTime) {
            endTime++;
        }

        List<Kline> returnList = new ArrayList<>();

        long nextPageTimeAdd = interval.toMilliseconds() * KLINES_MAX_NUM_PER_REQ;

        try {
            while (startTime < endTime) {

                String req = "GET " + ENDPOINT_KLINES + "?symbol=" + symbol + "&interval=" + interval + "&startTime="
                        + startTime + "&endTime=" + endTime + "&limit=" + KLINES_MAX_NUM_PER_REQ
                        + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n\r\n";

                JsonElement klinesPage = null; // TODO implement necessary method in api handler, refactor to not use
                                               // JsonElement here
                JsonArray klines = klinesPage.getAsJsonArray(); // TODO test with different type if throw exception and
                                                                // catch it
                for (JsonElement elem : klines) {
                    JsonArray kl = elem.getAsJsonArray();
                    returnList.add(new Kline(kl.get(0).getAsLong(), kl.get(1).getAsDouble(), kl.get(2).getAsDouble(),
                            kl.get(3).getAsDouble(), kl.get(4).getAsDouble(), kl.get(5).getAsDouble(),
                            kl.get(6).getAsLong(), kl.get(7).getAsDouble(), kl.get(8).getAsInt(),
                            kl.get(9).getAsDouble(), kl.get(10).getAsDouble()));
                }

                startTime += nextPageTimeAdd;
            }
        } catch (Exception e) {
            if (e instanceof ExchangeException) {
                throw e;
            } else {
                throw new ExchangeException(ExchangeException.ErrorType.PARSING_PROBLEM, "Problem parsing klines data",
                        "LiveDataProvider::getExchangeKlines", e);
            }
        }

        return returnList;
    }

    private List<Kline> getDBKlines(String symbol, Interval interval, long startTime, long endTime) {

        List<Kline> klines = database.getKlines(symbol, interval, startTime, endTime);

        if (klines.isEmpty()) {
            logger.warn("getDBKlines : Klines list empty");
        }

        return klines;
    }

    private void setDBKlines(String symbol, Interval interval, long startTimeIndex, List<Kline> klinesToAdd) {

        database.setKlines(symbol, interval, startTimeIndex, klinesToAdd);
    }

}
