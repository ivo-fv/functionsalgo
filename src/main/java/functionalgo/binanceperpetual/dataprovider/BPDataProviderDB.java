package functionalgo.binanceperpetual.dataprovider;

import java.util.List;

import functionalgo.datapoints.FundingRate;
import functionalgo.datapoints.Interval;
import functionalgo.datapoints.Kline;

public interface BPDataProviderDB {

    List<FundingRate> getFundingRates(String symbol, long startTime, long endTime);

    void setFundingRates(String symbol, long startTime, List<FundingRate> newFRates);

    List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime);

    void setKlines(String symbol, Interval interval, long startTimeIndex, List<Kline> klinesToAdd);
    
}
