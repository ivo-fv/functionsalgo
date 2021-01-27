package functionsalgo.samplestrat;

public class Position {
    String symbol;
    boolean isLong;
    double quantity;

    Position(String symbol, boolean isLong, double quantity) {
        this.symbol = symbol;
        this.isLong = isLong;
        this.quantity = quantity;
    }

}
