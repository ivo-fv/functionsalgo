package functionalgo.binanceperpetual.dataprovider;

import java.util.List;

import functionalgo.Database;
import functionalgo.Logger;
import functionalgo.binanceperpetual.BPLimitedTLSClient;
import functionalgo.datapoints.FundingRate;
import functionalgo.datapoints.Interval;
import functionalgo.datapoints.Kline;
import functionalgo.exceptions.ExchangeException;
import functionalgo.exceptions.NoDataAvailableException;

public class BPLiveDataProvider implements BPDataProvider {
    
    private static final String KLINES_TABLE = "BPLiveDataProviderKlines";
    private static final String FRATES_TABLE = "BPLiveDataProviderFundingRates";
    private static final Interval FUNDING_INTERVAL = Interval._8h;
    private static final long FRATES_LEEWAY = 100;
    
    private Database database;
    private Logger logger;
    private BPLimitedTLSClient restClient;
    
    public BPLiveDataProvider(Database database, Logger logger) throws ExchangeException {
        
        this.database = database;
        this.logger = logger;
        
        restClient = new BPLimitedTLSClient(logger);
        
        if (!database.containsTable(KLINES_TABLE)) {
            database.createTable(KLINES_TABLE);
        }
        if (!database.containsTable(FRATES_TABLE)) {
            database.createTable(FRATES_TABLE);
        }
    }
    
    @Override
    public double getFundingRate(String symbol, long timestamp) throws NoDataAvailableException {
        
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws NoDataAvailableException {
        
        // TODO no data throw
        if (startTime == endTime) {
            endTime++;
        }
        
        List<FundingRate> dbFRates = getDBFundingRates(symbol, startTime, endTime);
        
        if (dbFRates.isEmpty()) {
            
            List<FundingRate> newFRates = getExchangeFRates(symbol, startTime - FRATES_LEEWAY, endTime + FRATES_LEEWAY);
            long adjustedStartTime = (startTime / FUNDING_INTERVAL.toMilliseconds()) * FUNDING_INTERVAL.toMilliseconds();
            setDBFRates(symbol, adjustedStartTime, newFRates);
            return dbFRates;
            
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
    
    private List<FundingRate> getExchangeFRates(String symbol, long startTime, long endTime) {
        
        // TODO Auto-generated method stub
        return null;
    }
    
    private List<FundingRate> getDBFundingRates(String symbol, long startTime, long endTime) {
        
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
    public double getOpen(String symbol, long timestamp, Interval interval) throws NoDataAvailableException {
        
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public Kline getKline(String symbol, long timestamp, Interval interval) throws NoDataAvailableException {
        
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) throws NoDataAvailableException {
        
        // TODO no data throw
        if (startTime == endTime) {
            endTime++;
        }
        
        List<Kline> dbKlines = getDBKlines(symbol, interval, startTime, endTime);
        
        if (dbKlines.isEmpty()) {
            
            long adjustedStartTime = (startTime / interval.toMilliseconds()) * interval.toMilliseconds();
            List<Kline> newKlines = getExchangeKlines(symbol, interval, adjustedStartTime, endTime);
            setDBKlines(symbol, interval, adjustedStartTime, newKlines);
            return dbKlines;
            
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
    
    private List<Kline> getExchangeKlines(String symbol, Interval interval, long startTime, long endTime) {
        //TODO test
        
        
        
        
        // TODO Auto-generated method stub
        return null;
    }
    
    private List<Kline> getDBKlines(String symbol, Interval interval, long startTime, long endTime) {
        
        // TODO Auto-generated method stub
        return null;
    }
    
    private void setDBKlines(String symbol, Interval interval, long startTimeIndex, List<Kline> klinesToAdd) {
        
        // TODO Auto-generated method stub
    }
    
}
