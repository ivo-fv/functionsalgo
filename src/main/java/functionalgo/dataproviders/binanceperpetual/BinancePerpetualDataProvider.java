package functionalgo.dataproviders.binanceperpetual;

import functionalgo.DataProvider;

public interface BinancePerpetualDataProvider extends DataProvider {

    double getFundingRate(String longSymbol, long timestamp);

    double getOpen(String symbol, long timestamp);
    
}
