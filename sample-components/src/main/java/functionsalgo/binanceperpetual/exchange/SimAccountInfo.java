package functionsalgo.binanceperpetual.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import functionsalgo.datapoints.Timestamp;

class SimAccountInfo implements AccountInfo {

    class PositionData {

        static final String MARGIN_TYPE = "CROSSED";

        String symbol;
        boolean isLong;
        double avgOpenPrice;
        double currPrice;
        double quantity;
        double margin;

        PositionData(String symbol, boolean isLong, double avgOpenPrice, double quantity, double initialMargin) {

            this.symbol = symbol;
            this.isLong = isLong;
            this.avgOpenPrice = avgOpenPrice;
            this.currPrice = avgOpenPrice;
            this.quantity = quantity;
            this.margin = initialMargin;
        }
    }

    double marginBalance;
    double walletBalance;
    HashMap<String, Integer> leverages;
    HashMap<String, PositionData> longPositions;
    HashMap<String, PositionData> shortPositions;
    HashMap<String, Double> fundingRates;
    Timestamp lastUpdatedTime;
    double takerFee = SimExchange.TAKER_OPEN_CLOSE_FEE;
    double worstCurrentMarginBalance;
    HashMap<String, Double> ordersWithQuantities;
    ArrayList<OrderError> errors;

    SimAccountInfo(double walletBalance, Timestamp lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
        this.walletBalance = walletBalance;
        marginBalance = walletBalance;
        worstCurrentMarginBalance = walletBalance;
        longPositions = new HashMap<>();
        shortPositions = new HashMap<>();
        leverages = new HashMap<>();
        fundingRates = new HashMap<>();
        ordersWithQuantities = new HashMap<>();
    }

    @Override
    public long getTimestampMillis() {

        return lastUpdatedTime.getTime();
    }

    @Override
    public double getWalletBalance() {

        return walletBalance;
    }

    @Override
    public double getQuantity(String symbol, boolean isLong) {
        try {
            return isLong ? longPositions.get(symbol).quantity : shortPositions.get(symbol).quantity;
        } catch (NullPointerException e) {
            return 0;
        }
    }

    @Override
    public int getLeverage(String symbol) {

        return leverages.get(symbol);
    }

    @Override
    public double getTakerFee() {

        return takerFee;
    }

    @Override
    public double getMarginBalance() {

        return marginBalance;
    }

    @Override
    public double getAverageOpenPrice(String symbol, boolean isLong) {
        try {
            return isLong ? longPositions.get(symbol).avgOpenPrice : shortPositions.get(symbol).avgOpenPrice;
        } catch (NullPointerException e) {
            return 0;
        }
    }

    @Override
    public boolean isSymbolIsolated(String symbol, boolean isLong) {

        return false;
    }

    @Override
    public boolean isHedgeMode() {

        return true;
    }

    @Override
    public List<OrderError> getOrderErrors() {
        return errors;
    }

    @Override
    public double getTotalInitialMargin() {
        double marginUsed = 0;
        for (Map.Entry<String, PositionData> entry : longPositions.entrySet()) {
            marginUsed += entry.getValue().margin;
        }
        for (Map.Entry<String, PositionData> entry : shortPositions.entrySet()) {
            marginUsed += entry.getValue().margin;
        }
        return marginUsed;
    }
}
