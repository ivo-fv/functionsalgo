package functionsalgo.binanceperpetual.exchange.exceptions;

public class NoBalanceInAccountException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoBalanceInAccountException(String msg) {
        super(msg);
    }

}