package functionalgo.binanceperpetual.dataprovider;

import java.util.List;

import functionalgo.datapoints.FundingRate;
import functionalgo.datapoints.Interval;
import functionalgo.datapoints.Kline;
import functionalgo.exceptions.ExchangeException;

public interface BPDataProvider {
    
    // TODO single rate not needed
    double getFundingRate(String symbol, long timestamp) throws ExchangeException;
    
    List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) throws ExchangeException;
    
    long getFundingInterval();
    
    /**
     * @param symbol
     * @param timestamp
     * @return the open pice of symbol at timestamp
     */
    // TODO single open not needed
    double getOpen(String symbol, long timestamp, Interval interval) throws ExchangeException;
    
    // TODO single Kline probably not needed
    Kline getKline(String symbol, long timestamp, Interval interval) throws ExchangeException;
    
    List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) throws ExchangeException;
}
