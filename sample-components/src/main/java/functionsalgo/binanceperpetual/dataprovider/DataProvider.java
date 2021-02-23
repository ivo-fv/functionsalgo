package functionsalgo.binanceperpetual.dataprovider;

import java.util.Map;

import functionsalgo.binanceperpetual.FundingRate;
import functionsalgo.datapoints.Timestamp;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;

public interface DataProvider {

    Map<Long, FundingRate> getFundingRates(String symbol, Timestamp startTime, Timestamp endTime)
            throws ExchangeException;

    Interval getFundingInterval();

    Map<Long, Kline> getKlines(String symbol, Interval interval, Timestamp startTime, Timestamp endTime)
            throws ExchangeException;
}
