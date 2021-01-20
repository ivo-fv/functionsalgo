package functionalgo.binanceperpetual.dataprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import functionalgo.Logger;
import functionalgo.aws.DynamoDB;
import functionalgo.aws.LambdaLogger;
import functionalgo.binanceperpetual.BPLimitedTLSClient;
import functionalgo.datapoints.FundingRate;
import functionalgo.datapoints.Interval;
import functionalgo.datapoints.Kline;
import functionalgo.exceptions.ExchangeException;

// TODO use a proper http client library
public class BPLiveDataProvider implements BPDataProvider {
    
    /**
     * Used for quick testing
     */
    public static void main(String[] args) throws ExchangeException {
        
        Logger logger = new LambdaLogger();
        BPDataProviderDB database = new DynamoDB();
        
        BPLiveDataProvider dataProvider = new BPLiveDataProvider(database, logger);
        
        try {
            List<Kline> klines = dataProvider.getExchangeKlines("ETHUSDT", Interval._5m, 1598346800000L, 1599562800000L);
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
    
    private static final String HOST = BPLimitedTLSClient.HOST;
    private static final String ENDPOINT_KLINES = "/fapi/v1/klines";
    private static final int KLINES_MAX_NUM_PER_REQ = 1000;
    private static final String ENDPOINT_FRATES = "/fapi/v1/fundingRate";
    private static final int FRATES_MAX_NUM_PER_REQ = 500;
    private static final String KLINES_TABLE = "BPLiveDataProviderKlines";
    private static final String FRATES_TABLE = "BPLiveDataProviderFundingRates";
    private static final Interval FUNDING_INTERVAL = Interval._8h;
    private static final long FRATES_LEEWAY = 100;
    
    private BPDataProviderDB database;
    private Logger logger;
    private BPLimitedTLSClient restClient;
    
    public BPLiveDataProvider(BPDataProviderDB database, Logger logger) throws ExchangeException {
        
        this.database = database;
        this.logger = logger;
        
        restClient = new BPLimitedTLSClient(logger);
        
        database.createTableIfNotExist();
    }
    
    @Override
    public List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws ExchangeException {
        
        // TODO no data throw
        if (startTime == endTime) {
            endTime++;
        }
        
        List<FundingRate> dbFRates = getDBFundingRates(symbol, startTime, endTime);
        
        if (dbFRates.isEmpty()) {
            
            List<FundingRate> newFRates = getExchangeFRates(symbol, startTime - FRATES_LEEWAY, endTime + FRATES_LEEWAY);
            long adjustedStartTime = (startTime / FUNDING_INTERVAL.toMilliseconds()) * FUNDING_INTERVAL.toMilliseconds();
            setDBFRates(symbol, adjustedStartTime, newFRates);
            return newFRates;
            
        } else {
            long lastTime = (Math.min(endTime, System.currentTimeMillis()) / FUNDING_INTERVAL.toMilliseconds())
                    * FUNDING_INTERVAL.toMilliseconds();
            
            long lastFundingTime = (dbFRates.get(dbFRates.size() - 1).getFundingTime() / FUNDING_INTERVAL.toMilliseconds())
                    * FUNDING_INTERVAL.toMilliseconds();
            
            if (lastFundingTime < lastTime) {
                List<FundingRate> newFRates = getExchangeFRates(symbol, lastFundingTime - FRATES_LEEWAY, endTime + FRATES_LEEWAY);
                setDBFRates(symbol, lastFundingTime, newFRates);
                dbFRates.remove(dbFRates.size() - 1);
                dbFRates.addAll(newFRates);
            }
            
            return dbFRates;
        }
    }
    
    private List<FundingRate> getExchangeFRates(String symbol, long startTime, long endTime) throws ExchangeException {
        
        List<FundingRate> returnList = new ArrayList<>();
        
        long nextPageTimeAdd = FUNDING_INTERVAL.toMilliseconds() * FRATES_MAX_NUM_PER_REQ;
        
        try {
            while (startTime < endTime) {
                
                String req = "GET " + ENDPOINT_FRATES + "?symbol=" + symbol + "&startTime=" + startTime + "&endTime=" + endTime
                        + "&limit=" + FRATES_MAX_NUM_PER_REQ + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n\r\n";
                
                JsonElement fratesPage = restClient.apiRetrySendRequestGetParsedResponse(req);
                JsonArray klines = fratesPage.getAsJsonArray();
                for (JsonElement elem : klines) {
                    JsonObject fr = elem.getAsJsonObject();
                    returnList.add(new FundingRate(fr.get("symbol").getAsString(), fr.get("fundingRate").getAsDouble(),
                            fr.get("fundingTime").getAsLong()));
                }
                
                startTime += nextPageTimeAdd;
            }
        } catch (Exception e) {
            logger.log(-4, -1, e.toString(), Arrays.toString(e.getStackTrace()));
            if (e instanceof ExchangeException) {
                throw e;
            } else {
                throw new ExchangeException(-1, e.toString(), ExchangeException.PARSING_PROBLEM);
            }
        }
        
        return returnList;
    }
    
    private List<FundingRate> getDBFundingRates(String symbol, long startTime, long endTime) {
        
        
        
        
        // TODO test single frate
        // TODO decide to return null or empty in case of nothing found
        // TODO log if no data or problem
        // TODO Auto-generated method stub
        return null;
    }
    
    private void setDBFRates(String symbol, long startTime, List<FundingRate> newFRates) {
        
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public long getFundingInterval() {
        
        return FUNDING_INTERVAL.toMilliseconds();
    }
    
    @Override
    public List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) throws ExchangeException {
        
        // TODO no data throw
        if (startTime == endTime) {
            endTime++;
        }
        
        List<Kline> dbKlines = getDBKlines(symbol, interval, startTime, endTime);
        
        if (dbKlines.isEmpty()) {
            
            long adjustedStartTime = (startTime / interval.toMilliseconds()) * interval.toMilliseconds();
            List<Kline> newKlines = getExchangeKlines(symbol, interval, adjustedStartTime, endTime);
            setDBKlines(symbol, interval, adjustedStartTime, newKlines);
            return newKlines;
            
        } else {
            long lastTime = (Math.min(endTime, System.currentTimeMillis()) / interval.toMilliseconds())
                    * interval.toMilliseconds();
            
            long lastKlineOpenTime = dbKlines.get(dbKlines.size() - 1).getOpenTime();
            
            if (lastKlineOpenTime < lastTime) {
                List<Kline> newKlines = getExchangeKlines(symbol, interval, lastKlineOpenTime, endTime);
                setDBKlines(symbol, interval, lastKlineOpenTime, newKlines);
                dbKlines.remove(dbKlines.size() - 1);
                dbKlines.addAll(newKlines);
            }
            
            return dbKlines;
        }
    }
    
    private List<Kline> getExchangeKlines(String symbol, Interval interval, long startTime, long endTime)
            throws ExchangeException {
        
        List<Kline> returnList = new ArrayList<>();
        
        long nextPageTimeAdd = interval.toMilliseconds() * KLINES_MAX_NUM_PER_REQ;
        
        try {
            while (startTime < endTime) {
                
                String req = "GET " + ENDPOINT_KLINES + "?symbol=" + symbol + "&interval=" + interval + "&startTime=" + startTime
                        + "&endTime=" + endTime + "&limit=" + KLINES_MAX_NUM_PER_REQ + " HTTP/1.1\r\nConnection: close\r\nHost: "
                        + HOST + "\r\n\r\n";
                
                JsonElement klinesPage = restClient.apiRetrySendRequestGetParsedResponse(req);
                JsonArray klines = klinesPage.getAsJsonArray(); // TODO test with different type if throw exception and catch it
                for (JsonElement elem : klines) {
                    JsonArray kl = elem.getAsJsonArray();
                    returnList.add(new Kline(kl.get(0).getAsLong(), kl.get(1).getAsDouble(), kl.get(2).getAsDouble(),
                            kl.get(3).getAsDouble(), kl.get(4).getAsDouble(), kl.get(5).getAsDouble(), kl.get(6).getAsLong(),
                            kl.get(7).getAsDouble(), kl.get(8).getAsInt(), kl.get(9).getAsDouble(), kl.get(10).getAsDouble()));
                }
                
                startTime += nextPageTimeAdd;
            }
        } catch (Exception e) {
            logger.log(-4, -1, e.toString(), Arrays.toString(e.getStackTrace()));
            if (e instanceof ExchangeException) {
                throw e;
            } else {
                throw new ExchangeException(-1, e.toString(), ExchangeException.PARSING_PROBLEM);
            }
        }
        
        return returnList;
    }
    
    private List<Kline> getDBKlines(String symbol, Interval interval, long startTime, long endTime) {
        
        // TODO test single candle
        // TODO decide to return null or empty in case of nothing found
        // TODO log if no data or problem
        // TODO Auto-generated method stub
        return null;
    }
    
    private void setDBKlines(String symbol, Interval interval, long startTimeIndex, List<Kline> klinesToAdd) {
        
        // TODO Auto-generated method stub
    }
    
}
