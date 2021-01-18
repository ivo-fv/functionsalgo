package functionalgo.binanceperpetual.dataprovider;

import java.util.List;

import functionalgo.datapoints.FundingRate;
import functionalgo.datapoints.Interval;
import functionalgo.datapoints.Kline;
import functionalgo.exceptions.ExchangeException;

public interface BPDataProvider {
    
    List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws ExchangeException;
    
    long getFundingInterval();
    
    List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) throws ExchangeException;
}
