package functionalgo.binanceperpetual;

public class BPOrderResultWrapper {

    private String symbol;

    BPOrderResultWrapper(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
