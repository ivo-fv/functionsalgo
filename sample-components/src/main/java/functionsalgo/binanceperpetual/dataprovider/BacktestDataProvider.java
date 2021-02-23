package functionsalgo.binanceperpetual.dataprovider;

import java.util.Map;

import functionsalgo.binanceperpetual.FundingRate;
import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.datapoints.Timestamp;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;

public class BacktestDataProvider implements DataProvider {

    Map<Interval, HistoricKlines> klines;
    HistoricFundingRates fundRates;

    public BacktestDataProvider(Map<Interval, HistoricKlines> klinesPerInterval, HistoricFundingRates fundRates) {
        klines = klinesPerInterval;
        this.fundRates = fundRates;
    }

    @Override
    public Interval getFundingInterval() {

        return fundRates.getFundingInterval();
    }

    @Override
    public Map<Long, FundingRate> getFundingRates(String symbol, Timestamp startTime, Timestamp endTime)
            throws ExchangeException {

        return fundRates.getFundingRates(symbol);
    }

    @Override
    public Map<Long, Kline> getKlines(String symbol, Interval interval, Timestamp startTime, Timestamp endTime)
            throws ExchangeException {

        return klines.get(interval).getKlines(symbol);
    }
}
