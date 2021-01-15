package functionalgo.dataproviders.binanceperpetual;

import java.util.List;

import functionalgo.Database;
import functionalgo.datapoints.Kline;
import functionalgo.exceptions.NoDataAvailableException;

public class BPLiveDataProvider implements BPDataProvider {
    
    Database database;
    
    public BPLiveDataProvider(Database database) {
        
        this.database = database;
    }
    
    @Override
    public double getFundingRate(String symbol, long timestamp) throws NoDataAvailableException {
        
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public double getPeriodFundingRates(String symbol, long startTime, long endTime) throws NoDataAvailableException {
        
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public long getFundingInterval() {
        
        // TODO Auto-generated method stub
        return 0;
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
    public List<Kline> getKlines(String symbol, long startTime, long endTime, Interval interval) throws NoDataAvailableException {
        
        // 1.a retrieve as many 1m candles necessary to form a interval candle
        // 1.b1 if database doesn't have the candles, get them from data collector that grabs them from the exchange
        // 1.b2 insert the candles in the database
        // 2. merge the 1m candles to match the interval requested
        // 3. return a list of the candles
        
        // TODO Auto-generated method stub
        return null;
    }
    
}
