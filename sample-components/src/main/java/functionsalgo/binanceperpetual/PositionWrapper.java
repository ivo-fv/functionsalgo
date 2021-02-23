package functionsalgo.binanceperpetual;

public class PositionWrapper {
    public String symbol;
    public double quantity;
    public double averagePrice;
    public boolean isIsolated;
    public boolean isBoth;
    public boolean isLong;
    public double currPrice;
    public double margin;

    public PositionWrapper(String symbol, boolean isLong, double avgOpenPrice, double quantity, double initialMargin) {
        this.symbol = symbol;
        this.isLong = isLong;
        this.averagePrice = avgOpenPrice;
        this.quantity = quantity;
        this.margin = initialMargin;
    }

    public PositionWrapper(String symbol, double quantity, double averagePrice, boolean isIsolated, boolean isBoth,
            boolean isLong) {

        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.isIsolated = isIsolated;
        this.isBoth = isBoth;
        this.isLong = isLong;
    }
}
