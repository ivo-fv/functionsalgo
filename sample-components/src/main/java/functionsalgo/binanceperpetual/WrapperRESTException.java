package functionsalgo.binanceperpetual;

import functionsalgo.exceptions.ExchangeException;

public class WrapperRESTException extends ExchangeException {

    private static final long serialVersionUID = 1L;

    public static final int BAD_LEVERAGE = -10;
    public static final int UNKNOWN_RESPONSE = -2;
    public static final int UNKNOWN_ERROR = -1;
    public static final int PARSE_ERROR = -3;
    public static final int CONNECTION_ERROR = -4;
    public static final int BAD_RESPONSE_TO_PARSE = -5;

    public static final int LOCAL_ERROR_BOUNDARY = 99;

    public WrapperRESTException(int code, String responseMsg, String exceptionInfo) {
        super(code, responseMsg, exceptionInfo);
    }

}
