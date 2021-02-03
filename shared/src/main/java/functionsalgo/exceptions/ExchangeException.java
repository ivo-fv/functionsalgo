package functionsalgo.exceptions;

public class ExchangeException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum ErrorType {
        INIT_FAILED, PARAM_PROBLEM, NOT_FIXABLE, ORDER_FAILED, API_ERROR, PARSING_PROBLEM, INVALID_STATE
    }

    int code;
    Enum<?> errorType;
    String responseMsg;
    String exceptionInfo;
    Throwable wrappedException;

    public ExchangeException(int code, String responseMsg, String exceptionInfo) {
        super(code + " ; " + responseMsg + " ; " + exceptionInfo);
        this.code = code;
        this.responseMsg = responseMsg;
        this.exceptionInfo = exceptionInfo;
    }

    public ExchangeException(int code, String responseMsg, String exceptionInfo, Throwable e) {
        super(code + " ; " + responseMsg + " ; " + exceptionInfo + " ; " + e.toString());
        this.code = code;
        this.responseMsg = responseMsg;
        this.exceptionInfo = exceptionInfo;
        this.wrappedException = e;
    }

    public ExchangeException(Enum<?> errorType, String msg1, String msg2) {
        super(errorType + " ; " + msg1 + " ; " + msg2);
        this.errorType = errorType;
        this.responseMsg = msg1;
        this.exceptionInfo = msg2;
    }

    public ExchangeException(Enum<?> errorType, String msg1, String msg2, Throwable e) {
        super(errorType + " ; " + msg1 + " ; " + msg2 + " ; " + e.toString());
        this.errorType = errorType;
        this.responseMsg = msg1;
        this.exceptionInfo = msg2;
        this.wrappedException = e;
    }

    public int getCode() {

        return code;
    }

    public Enum<?> getErrorType() {
        return errorType;
    }

    public void setErrorType(Enum<?> errorType) {
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
