package functionsalgo.binanceperpetual.exchange;

import java.util.HashMap;
import java.util.List;

import functionsalgo.exceptions.ExchangeException;

class OLDLiveAccountInfo implements AccountInfo {

    class PositionData {

        double quantity;
        double avgOpenPrice;

        PositionData(double quantity, double avgOpenPrice) {

            this.quantity = quantity;
            this.avgOpenPrice = avgOpenPrice;
        }
    }

    class SymbolData {

        double fundingRate;
        double markPrice;
        long nextFundingTime;

        SymbolData(double fundingRate, double markPrice, long nextFundingTime) {

            this.fundingRate = fundingRate;
            this.markPrice = markPrice;
            this.nextFundingTime = nextFundingTime;
        }
    }

    static final double TAKER_FEE = 0.0004;

    double totalInitialMargin = 0;
    double marginBalance = 0;
    double walletBalance = 0;
    long timestamp;
    boolean isHedgeMode;
    boolean isBalancesDesynch;
    boolean isPositionsDesynch;
    HashMap<String, Integer> leverages;
    HashMap<String, Boolean> isSymbolIsolated;
    HashMap<String, PositionData> longPositions;
    HashMap<String, PositionData> shortPositions;
    HashMap<String, PositionData> bothPositions;
    HashMap<String, SymbolData> symbolData;
    HashMap<String, ExchangeException> ordersWithErrors;
    HashMap<String, Double> ordersWithQuantities;

    public OLDLiveAccountInfo() {

        leverages = new HashMap<>();
        isSymbolIsolated = new HashMap<>();
        longPositions = new HashMap<>();
        shortPositions = new HashMap<>();
        bothPositions = new HashMap<>();
        symbolData = new HashMap<>();
        ordersWithErrors = new HashMap<>();
        ordersWithQuantities = new HashMap<>();
    }

    @Override
    public double getQuantity(String symbol, boolean isLong) {

        if (isLong) {
            return longPositions.get(symbol).quantity;
        } else {
            return shortPositions.get(symbol).quantity;
        }
    }

    @Override
    public long getTimestamp() {

        return timestamp;
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

        return TAKER_FEE;
    }

    @Override
    public double getMarginBalance() {

        return marginBalance;
    }

    @Override
    public double getAverageOpenPrice(String symbol, boolean isLong) {

        if (isLong) {
            return longPositions.get(symbol).avgOpenPrice;
        } else {
            return shortPositions.get(symbol).avgOpenPrice;
        }
    }

    @Override
    public double getWorstCurrenttMarginBalance() {

        return marginBalance;
    }

    public long getNextFundingTime(String symbol) {

        return symbolData.get(symbol).nextFundingTime;
    }

    public double getFundingRate(String symbol) {

        return symbolData.get(symbol).fundingRate;
    }

    public double getMarkPrice(String symbol) {

        return symbolData.get(symbol).markPrice;
    }

    public ExchangeException getOrderError(String orderId) {

        return ordersWithErrors.get(orderId);
    }

    public double getOrderQuantity(String orderId) {

        return ordersWithQuantities.get(orderId);
    }

    public boolean isBalancesDesynch() {

        return isBalancesDesynch;
    }

    public boolean isPositionsDesynch() {

        return isPositionsDesynch;
    }

    @Override
    public boolean isSymbolIsolated(String symbol, boolean isLong) {

        return isSymbolIsolated.get(symbol);
    }

    @Override
    public boolean isHedgeMode() {

        return isHedgeMode;
    }

    @Override
    public List<OrderError> getOrderErrors() {
        // TODO Auto-generated method stub
        return null;
    }
}
