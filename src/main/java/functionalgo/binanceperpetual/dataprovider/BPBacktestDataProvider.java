package functionalgo.binanceperpetual.dataprovider;

import java.util.HashMap;
import java.util.List;

import functionalgo.datapoints.FundingRate;
import functionalgo.datapoints.Interval;
import functionalgo.datapoints.Kline;
import functionalgo.exceptions.NoDataAvailableException;

public class BPBacktestDataProvider implements BPDataProvider {
    
    HashMap<Interval, BPHistoricKlines> klines;
    BPHistoricFundingRates fundRates;
    
    public BPBacktestDataProvider(Interval[] intervals) {
        
        klines = new HashMap<>();
        for (Interval interval : intervals) {
            klines.put(interval, BPHistoricKlines.loadKlines(interval));
        }
        
        fundRates = BPHistoricFundingRates.loadFundingRates();
    }
    
    @Override
    public double getFundingRate(String symbol, long timestamp) throws NoDataAvailableException {
        
        try {
            return fundRates.getRate(symbol, timestamp);
        } catch (NullPointerException e) {
            throw new NoDataAvailableException("No funding rate data for the " + symbol + timestamp + "param combination.");
        }
        
    }
    
    @Override
    public double getOpen(String symbol, long timestamp, Interval interval) throws NoDataAvailableException {
        
        try {
            return klines.get(interval).getOpen(symbol, timestamp);
        } catch (NullPointerException e) {
            throw new NoDataAvailableException(
                    "No open price data for the " + symbol + timestamp + interval + "param combination.");
        }
    }
    
    @Override
    public long getFundingInterval() {
        
        return fundRates.getFundingIntervalMillis();
    }
    
    @Override
    public List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws NoDataAvailableException {
        
        // TODO to implement after live
        return null;
    }
    
    @Override
    public Kline getKline(String symbol, long timestamp, Interval interval) throws NoDataAvailableException {
        
        // TODO to implement after live
        return null;
    }
    
    @Override
    public List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) throws NoDataAvailableException {
        
        // TODO to implement after live
        return null;
    }
    
}
