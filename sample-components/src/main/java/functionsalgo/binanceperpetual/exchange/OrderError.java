package functionsalgo.binanceperpetual.exchange;

import functionsalgo.exceptions.ExchangeException;

public class OrderError {

    public enum OrderStatus {
        UNKNOWN, FAILED, NOT_ENOUGH_MARGIN_FAILED, NO_SUCH_POSITION_FAILED
    }

    int orderId;
    OrderStatus status;
    ExchangeException exception;

    public OrderError(int orderId, OrderStatus status, ExchangeException exception) {
        super();
        this.orderId = orderId;
        this.status = status;
        this.exception = exception;
    }

    public int getOrderId() {
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
