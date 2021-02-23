package functionsalgo.binanceperpetual.exchange;

class BatchedOrder {
    int orderId;
    String symbol;
    boolean isLong;
    double quantity;
    boolean isOpen;

    public BatchedOrder(int orderId, String symbol, boolean isLong, double quantity, boolean isOpen) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.isLong = isLong;
        this.quantity = quantity;
        this.isOpen = isOpen;
    }

    @Override
    public String toString() {
        return "BatchedOrder [orderId=" + orderId + ", symbol=" + symbol + ", isLong=" + isLong + ", quantity="
                + quantity + ", isOpen=" + isOpen + "]";
    }
}
