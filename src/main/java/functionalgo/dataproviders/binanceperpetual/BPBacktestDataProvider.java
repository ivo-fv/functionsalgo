package functionalgo.dataproviders.binanceperpetual;

import java.util.HashMap;

import functionalgo.exceptions.NoDataAvailableException;

public class BPBacktestDataProvider implements BPDataProvider {
    
    HashMap<Interval, BPHistoricKlines> klines;
    BPHistoricFundingRates fundRates;
    
    public BPBacktestDataProvider() {
        
        klines = new HashMap<>();
        for (Interval interval : Interval.values()) {
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
    
}
