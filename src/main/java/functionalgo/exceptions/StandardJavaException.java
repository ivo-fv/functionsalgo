package functionalgo.exceptions;

public class StandardJavaException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private Throwable cause;
    
    public StandardJavaException(Exception cause) {
        
        // TODO log exception
        this.cause = cause;
    }
    
    @Override
    public Throwable getCause() {
        
        return cause;
    }
}
