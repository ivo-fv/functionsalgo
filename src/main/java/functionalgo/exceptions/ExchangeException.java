package functionalgo.exceptions;

public class ErrorParsingJsonException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public ErrorParsingJsonException(String msg) {
        
        super(msg);
    }
}