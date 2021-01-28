package functionsalgo.binanceperpetual.exchange.exceptions;

public class SymbolQuantityTooLow extends Exception {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private double symbolQty;
    private double stepSize;

    public SymbolQuantityTooLow(String symbol, double symbolQty, double stepSize) {
        super("The symbol: " + symbol + " requires a minimum order of " + stepSize + " but was " + symbolQty);
        this.symbol = symbol;
        this.symbolQty = symbolQty;
        this.stepSize = stepSize;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getSymbolQty() {
        return symbolQty;
    }

    public double getStepSize() {
        return stepSize;
    }

    @Override
    public String toString() {
        return "SymbolNotTradingException [symbol=" + symbol + "]";
    }
}