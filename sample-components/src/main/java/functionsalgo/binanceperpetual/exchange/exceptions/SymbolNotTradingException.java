package functionsalgo.binanceperpetual.exchange.exceptions;

public class SymbolNotTradingException extends Exception {

    private static final long serialVersionUID = 1L;

    private String symbol;

    public SymbolNotTradingException(String symbol) {
        super("The symbol: " + symbol + " is not currently trading.");
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return "SymbolNotTradingException [symbol=" + symbol + "]";
    }
}
