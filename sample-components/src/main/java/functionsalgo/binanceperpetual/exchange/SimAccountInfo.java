package functionsalgo.binanceperpetual.exchange;

import java.util.HashMap;
import java.util.List;

import functionsalgo.exceptions.ExchangeException;

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
    HashMap<String, PositionData> positions;
    HashMap<String, Double> fundingRates;
    long lastUpdatedTime;
    public long nextFundingTime;
    double takerFee = SimExchange.TAKER_OPEN_CLOSE_FEE;
    double worstCurrentMarginBalance;
    HashMap<String, ExchangeException> ordersWithErrors;
    HashMap<String, Double> ordersWithQuantities;

    SimAccountInfo(double walletBalance) {

        this.walletBalance = walletBalance;
        marginBalance = walletBalance;
        worstCurrentMarginBalance = walletBalance;

        positions = new HashMap<>();
        leverages = new HashMap<>();
        fundingRates = new HashMap<>();
        ordersWithErrors = new HashMap<>();
        ordersWithQuantities = new HashMap<>();

        lastUpdatedTime = 0;
    }

    @Override
    public long getTimestamp() {

        return lastUpdatedTime;
    }

    @Override
    public double getWalletBalance() {

        return walletBalance;
    }

    @Override
    public double getQuantity(String symbol, boolean isLong) {

        return positions.get(SimExchange.getPositionId(symbol, isLong)).quantity;
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

        return positions.get(SimExchange.getPositionId(symbol, isLong)).avgOpenPrice;
    }

    @Override
    public double getWorstCurrenttMarginBalance() {

        return worstCurrentMarginBalance;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getTotalInitialMargin() {
        // TODO Auto-generated method stub
        return 0;
    }
}
