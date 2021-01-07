package functionalgo.dataproviders.binanceperpetual;

import functionalgo.DataProvider;

public interface BPDataProvider extends DataProvider {
    
    double getFundingRate(String symbol, long timestamp);
    
    /**
     * @param symbol
     * @param timestamp
     * @return the open pice of symbol at timestamp. If symbol does not have information returns
     *         Double.NEGATIVE_INFINITY
     */
    double getOpen(String symbol, long timestamp, Interval interval);
    
    long getFundingInterval();
    
    /**
     * Return 0 if not a backtest data provider.
     * 
     * @return
     */
    long getEarliestTimestamp();
    
    /**
     * Return 0 if not a backtest data provider.
     * 
     * @return
     */
    long getLatestTimestamp();
    
}
