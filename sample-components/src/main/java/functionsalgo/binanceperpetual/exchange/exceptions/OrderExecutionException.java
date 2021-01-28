package functionsalgo.binanceperpetual.exchange.exceptions;

import java.util.ArrayList;

import functionsalgo.binanceperpetual.WrapperRESTException;
import functionsalgo.binanceperpetual.exchange.OrderError;

public class OrderExecutionException extends Exception {

    private static final long serialVersionUID = 1L;

    ArrayList<OrderError> errors;
    WrapperRESTException wrappedException;

    public OrderExecutionException(ArrayList<OrderError> errors, WrapperRESTException wrappedException) {
        super("OrderExecutionException [errors=" + errors + ", wrappedException=" + wrappedException + "]");
        this.errors = errors;
        this.wrappedException = wrappedException;
    }

    public ArrayList<OrderError> getErrors() {
        return errors;
    }

    public WrapperRESTException getWrappedException() {
        return wrappedException;
    }

    @Override
    public String toString() {
        return "OrderExecutionException [errors=" + errors + ", wrappedException=" + wrappedException + "]";
    }
}
