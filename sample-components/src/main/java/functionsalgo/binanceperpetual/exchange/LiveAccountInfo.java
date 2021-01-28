package functionsalgo.binanceperpetual.exchange;

import java.util.List;
import java.util.Map;

import functionsalgo.binanceperpetual.PositionWrapper;

public class LiveAccountInfo implements AccountInfo {

    double marginBalance;
    double walletBalance;
    Map<String, Integer> leverages;
    Map<String, PositionWrapper> longPositions;
    Map<String, PositionWrapper> shortPositions;
    boolean isHedgeMode;
    long exchangeTime;
    double totalInitialMargin;
    List<OrderError> errors;

    @Override
    public double getTotalInitialMargin() {
        return totalInitialMargin;
    }

    @Override
    public long getTimestamp() {
        return exchangeTime;
    }

    @Override
    public double getWalletBalance() {
        return walletBalance;
    }

    @Override
    public int getLeverage(String symbol) {
        return leverages.get(symbol);
    }

    @Override
    public double getTakerFee() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getMarginBalance() {
        return marginBalance;
    }

    @Override
    public double getQuantity(String symbol, boolean isLong) {
        return isLong ? longPositions.get(symbol).quantity : shortPositions.get(symbol).quantity;
    }

    @Override
    public double getAverageOpenPrice(String symbol, boolean isLong) {
        return isLong ? longPositions.get(symbol).averagePrice : shortPositions.get(symbol).averagePrice;
    }

    @Override
    public double getWorstCurrenttMarginBalance() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<OrderError> getOrderErrors() {
        return errors;
    }

    @Override
    public boolean isSymbolIsolated(String symbol, boolean isLong) {
        return isLong ? longPositions.get(symbol).isIsolated : shortPositions.get(symbol).isIsolated;
    }

    @Override
    public boolean isHedgeMode() {
        return isHedgeMode;
    }

}
