package functionsalgo.binanceperpetual;

import functionsalgo.exceptions.ExchangeException;

public class WrapperRESTException extends ExchangeException {

    private static final long serialVersionUID = 1L;

    public enum ErrorType {
        BAD_LEVERAGE, UNKNOWN_RESPONSE, UNKNOWN_ERROR, PARSE_ERROR, CONNECTION_ERROR, BAD_RESPONSE_TO_PARSE,
        INCONSISTENT_ORDER_RESULT
    }

    public WrapperRESTException(int code, String responseMsg, String exceptionInfo) {
        super(code, responseMsg, exceptionInfo);
    }

    public WrapperRESTException(Enum<?> errorType, String msg1, String msg2) {
        super(errorType, msg1, msg2);
    }

}
