package functionsalgo.binanceperpetual.dataprovider;

import java.util.List;

import functionsalgo.datapoints.FundingRate;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;

public interface DataProviderDB {

    List<FundingRate> getFundingRates(String symbol, long startTime, long endTime);

    void setFundingRates(String symbol, long startTime, List<FundingRate> newFRates);

    List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime);

    void setKlines(String symbol, Interval interval, long startTimeIndex, List<Kline> klinesToAdd);
    
}
