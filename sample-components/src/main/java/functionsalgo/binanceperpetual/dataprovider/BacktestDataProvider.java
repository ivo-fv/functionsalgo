package functionsalgo.binanceperpetual.dataprovider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import functionsalgo.binanceperpetual.FundingRate;
import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;

public class BacktestDataProvider implements DataProvider {

    Map<Interval, HistoricKlines> klines;
    HistoricFundingRates fundRates;

    public BacktestDataProvider(Interval[] intervals) throws StandardJavaException {

        klines = new HashMap<>();
        for (Interval interval : intervals) {
            klines.put(interval, HistoricKlines.loadKlines(interval));
        }

        fundRates = HistoricFundingRates.loadFundingRates();
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
    public List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime)
            throws ExchangeException {

        return klines.get(interval).getKlines(symbol, startTime, endTime);
    }

    public Map<String, Kline> getKlinesMult(List<String> symbol, Interval interval, long startTime, long endTime)
            throws ExchangeException {
        // TODO thread pool fetch each each interval for each symbol, consider removing
        // checks from HistoricKlines and HistoricFundingRates, do them here
        // return klines.get(interval).getKlines(symbol, startTime, endTime);
        return null;
    }

}
