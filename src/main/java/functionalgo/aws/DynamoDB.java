package functionalgo.aws;

import functionalgo.binanceperpetual.dataprovider.BPDataProviderDB;
import functionalgo.samplestrat.SampleStratDB;

public class DynamoDB implements BPDataProviderDB, SampleStratDB { // TODO maybe have a GeneralStratDB, arbitrary value atribute
    
    @Override
    public void createTableIfNotExist() {
        // TODO find another signature for this method to know the table's purpose
        // TODO Auto-generated method stub
        
    }
    // TODO implement
    
    @Override
    public void createTableIfNotExist(String stratName) {
        
        // TODO Auto-generated method stub
        
    }
}
