package functionalgo.exceptions;

public class ExchangeException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public static final String PARAM_PROBLEM = "PARAM_PROBLEM";
    public static final String NOT_FIXABLE = "NOT_FIXABLE";
    public static final String ORDER_FAILED = "ORDER_FAILED";
    public static final String API_ERROR = "API_ERROR";
    public static final String PARSING_PROBLEM = "PARSING_PROBLEM";
    public static final String INVALID_STATE = "INVALID_STATE";
    
    int code;
    String responseMsg;
    String exceptionInfo;
    
    public ExchangeException(int code, String responseMsg, String exceptionInfo) {
        
        super(code + " ; " + responseMsg + " ; " + exceptionInfo);
        this.code = code;
        this.responseMsg = responseMsg;
        this.exceptionInfo = exceptionInfo;
    }
    
    public int getCode() {
        
        return code;
    }
    
    public String getResponseMsg() {
        
        return responseMsg;
    }
    
    public String getExceptionInfo() {
        
        return exceptionInfo;
    }
}
