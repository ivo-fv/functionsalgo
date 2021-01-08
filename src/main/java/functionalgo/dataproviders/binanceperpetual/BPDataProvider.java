package functionalgo.dataproviders.binanceperpetual;

import functionalgo.DataProvider;
import functionalgo.exceptions.NoDataAvailableException;

public interface BPDataProvider extends DataProvider {
    
    double getFundingRate(String symbol, long timestamp) throws NoDataAvailableException;
    
    /**
     * @param symbol
     * @param timestamp
     * @return the open pice of symbol at timestamp
     */
    double getOpen(String symbol, long timestamp, Interval interval) throws NoDataAvailableException;
    
    long getFundingInterval();
    
}
