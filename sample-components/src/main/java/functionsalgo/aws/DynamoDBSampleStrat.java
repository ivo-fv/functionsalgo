package functionsalgo.aws;

public class DynamoDBSampleStrat implements SampleStratDB {
    
    DynamoDBCommon db;
    
    public DynamoDBSampleStrat(DynamoDBCommon db) {
        
        this.db = db;
        // TODO if table not exist create table
    }    
}
