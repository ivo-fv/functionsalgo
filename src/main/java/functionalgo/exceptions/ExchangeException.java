package functionalgo.exceptions;

public class ExchangeException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    int code;
    String responseMsg;
    String exceptionMsg;
    
    public ExchangeException(int code, String responseMsg, String exceptionMsg) {
        
        super(code + ";" + responseMsg + ";" + exceptionMsg);
        this.code = code;
        this.responseMsg = responseMsg;
        this.exceptionMsg = exceptionMsg;
    }
    
    public int getCode() {
        
        return code;
    }
    
    public String getResponseMsg() {
        
        return responseMsg;
    }
    
    public String getExceptionMsg() {
        
        return exceptionMsg;
    }
}
