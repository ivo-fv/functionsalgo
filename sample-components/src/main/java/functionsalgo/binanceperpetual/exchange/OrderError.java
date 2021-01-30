package functionsalgo.binanceperpetual.exchange;

import functionsalgo.exceptions.ExchangeException;

public class OrderError {

    public static final String UNKNOWN = "UNKNOWN";
    public static final String FAILED = "FAILED";

    String orderId;
    String status;
    ExchangeException exception;

    public OrderError(String orderId, String status, ExchangeException exception) {
        super();
        this.orderId = orderId;
        this.status = status;
        this.exception = exception;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStatus() {
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
