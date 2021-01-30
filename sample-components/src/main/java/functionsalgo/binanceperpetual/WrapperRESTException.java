package functionsalgo.binanceperpetual;

import functionsalgo.exceptions.ExchangeException;

public class WrapperRESTException extends ExchangeException {

    private static final long serialVersionUID = 1L;

    public static final String BAD_LEVERAGE = "BAD_LEVERAGE";
    public static final String UNKNOWN_RESPONSE = "UNKNOWN_RESPONSE";
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    public static final String PARSE_ERROR = "PARSE_ERROR";
    public static final String CONNECTION_ERROR = "CONNECTION_ERROR";
    public static final String BAD_RESPONSE_TO_PARSE = "BAD_RESPONSE_TO_PARSE";
    public static final String INCONSISTENT_ORDER_RESULT = "INCONSISTENT_ORDER_RESULT";

    public WrapperRESTException(int code, String responseMsg, String exceptionInfo) {
        super(code, responseMsg, exceptionInfo);
    }

    public WrapperRESTException(String errorType, String msg1, String msg2) {
        super(errorType, msg1, msg2);
    }

}
