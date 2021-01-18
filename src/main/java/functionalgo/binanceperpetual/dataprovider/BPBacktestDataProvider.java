package functionalgo.binanceperpetual.dataprovider;

import java.util.HashMap;
import java.util.List;

import functionalgo.datapoints.FundingRate;
import functionalgo.datapoints.Interval;
import functionalgo.datapoints.Kline;
import functionalgo.exceptions.ExchangeException;

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
    public long getFundingInterval() {
        
        return fundRates.getFundingIntervalMillis();
    }
    
    @Override
    public List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws ExchangeException {
        
        return fundRates.getFundingRates(symbol, startTime, endTime);
    }
    
    @Override
    public List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) throws ExchangeException {
        
        return klines.get(interval).getKlines(symbol, startTime, endTime);
    }
    
}
