package functionsalgo.samplestrat;

public class Position {
    String symbol;
    boolean isLong;
    double quantity;
    public String id;

    Position(String id, String symbol, boolean isLong, double quantity) {
        this.id = id;
        this.symbol = symbol;
        this.isLong = isLong;
        this.quantity = quantity;
    }
}