package functionsalgo.aws;

import java.util.List;

import functionsalgo.binanceperpetual.FundingRate;
import functionsalgo.binanceperpetual.dataprovider.DataProviderDB;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;

public class DynamoDBBPDataProvider implements DataProviderDB {
    
    DynamoDBCommon db;
    
    public DynamoDBBPDataProvider(DynamoDBCommon db) {
        
        this.db = db;
        // TODO if table not exist create table
    }

    @Override
    public List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) {
        return null;
        
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setFundingRates(String symbol, long startTime, List<FundingRate> newFRates) {
        
        
        
        
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<Kline> getKlines(String symbol, Interval interval, long startTime, long endTime) {
        
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setKlines(String symbol, Interval interval, long startTimeIndex, List<Kline> klinesToAdd) {
        
        // TODO Auto-generated method stub
        
    }
    
}
