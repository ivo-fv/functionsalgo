package functionalgo.exceptions;

public class NoDataAvailableException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public NoDataAvailableException(String msg) {
        
        super(msg);
    }
}
