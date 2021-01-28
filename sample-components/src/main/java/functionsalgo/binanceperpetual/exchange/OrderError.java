package functionsalgo.binanceperpetual.exchange;

import functionsalgo.exceptions.ExchangeException;

public class OrderError {

    public static final int UNKNOWN = -1;
    public static final int FAILED = -2;

    String orderId;
    int status;
    ExchangeException exception;

    public OrderError(String orderId, int status, ExchangeException exception) {
        super();
        this.orderId = orderId;
        this.status = status;
        this.exception = exception;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getStatus() {
        return status;
    }

    public ExchangeException getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "OrderError [orderId=" + orderId + ", status=" + status + ", exception=" + exception + "]";
    }
}
