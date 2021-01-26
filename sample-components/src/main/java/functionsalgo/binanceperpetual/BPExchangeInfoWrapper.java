package functionsalgo.binanceperpetual;

import java.util.Map;

public class BPExchangeInfoWrapper {

    private Map<String, Boolean> symbolTrading;
    private Map<String, Double> symbolQtyStepSize;
    private long exchangeTime;

    BPExchangeInfoWrapper(Map<String, Boolean> symbolTrading, Map<String, Double> symbolQtyStepSize,
            long exchangeTime) {

        this.symbolTrading = symbolTrading;
        this.symbolQtyStepSize = symbolQtyStepSize;
        this.exchangeTime = exchangeTime;
    }

    public boolean getIsSymbolTrading(String symbol) {
        return symbolTrading.get(symbol);
    }

    public double getSymbolQtyStepSize(String symbol) {
        return symbolQtyStepSize.get(symbol);
    }

    public long getExchangeTime() {
        return exchangeTime;
    }

}
