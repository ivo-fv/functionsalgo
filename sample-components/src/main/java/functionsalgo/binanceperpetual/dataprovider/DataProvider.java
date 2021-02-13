package functionsalgo.binanceperpetual.dataprovider;

import java.util.List;

import functionsalgo.binanceperpetual.FundingRate;
import functionsalgo.datapoints.AdjustedTimestamp;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;

public interface DataProvider {

    List<FundingRate> getFundingRates(String symbol, AdjustedTimestamp startTime, AdjustedTimestamp endTime)
            throws ExchangeException;

    Interval getFundingInterval();

    List<Kline> getKlines(String symbol, Interval interval, AdjustedTimestamp startTime, AdjustedTimestamp endTime)
            throws ExchangeException;
}
