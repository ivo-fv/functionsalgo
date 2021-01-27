package functionsalgo.binanceperpetual.dataprovider;

import java.util.List;

import functionsalgo.datapoints.FundingRate;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;

public interface DataProvider {
    
    List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws ExchangeException;
    
    long getFundingInterval();
    
    List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) throws ExchangeException;
}
