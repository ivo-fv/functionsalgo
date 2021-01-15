package functionalgo;

public interface Database {

    boolean containsTable(String tableName);

    void createTable(String tableName);
    
}
