package functionsalgo.exceptions;

public class ExchangeException extends Exception {

    private static final long serialVersionUID = 1L;

    public static final String PARAM_PROBLEM = "PARAM_PROBLEM";
    public static final String NOT_FIXABLE = "NOT_FIXABLE";
    public static final String ORDER_FAILED = "ORDER_FAILED";
    public static final String API_ERROR = "API_ERROR";
    public static final String PARSING_PROBLEM = "PARSING_PROBLEM";
    public static final String INVALID_STATE = "INVALID_STATE";

    public static final int INIT_FAILED = 1;

    int code;
    String errorType;
    String responseMsg;
    String exceptionInfo;
    Throwable wrappedException;

    public ExchangeException(int code, String responseMsg, String exceptionInfo) {
        this(code, responseMsg, exceptionInfo, null);
    }

    public ExchangeException(String errorType, String msg1, String msg2) {
        super(errorType + " ; " + msg1 + " ; " + msg2);
        this.errorType = errorType;
        this.responseMsg = msg1;
        this.exceptionInfo = msg2;
    }

    public ExchangeException(int code, String responseMsg, String exceptionInfo, Throwable e) {
        super(code + " ; " + responseMsg + " ; " + exceptionInfo);
        this.code = code;
        this.responseMsg = responseMsg;
        this.exceptionInfo = exceptionInfo;
        this.wrappedException = e;
    }

    public int getCode() {

        return code;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getResponseMsg() {

        return responseMsg;
    }

    public String getExceptionInfo() {

        return exceptionInfo;
    }

    public Throwable getWrappedException() {
        return wrappedException;
    }

    @Override
    public String toString() {
        return "ExchangeException [code=" + code + ", errorType=" + errorType + ", responseMsg=" + responseMsg
                + ", exceptionInfo=" + exceptionInfo + ", wrappedException=" + wrappedException + "]";
    }
}
