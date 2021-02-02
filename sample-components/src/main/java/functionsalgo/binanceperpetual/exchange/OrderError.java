package functionsalgo.binanceperpetual.exchange;

import functionsalgo.exceptions.ExchangeException;

public class OrderError {

    public enum OrderStatus {
        UNKNOWN, FAILED
    }

    String orderId;
    OrderStatus status;
    ExchangeException exception;

    public OrderError(String orderId, OrderStatus status, ExchangeException exception) {
        super();
        this.orderId = orderId;
        this.status = status;
        this.exception = exception;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
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
