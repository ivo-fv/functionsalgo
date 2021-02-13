package functionsalgo.binanceperpetual.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.binanceperpetual.exchange.exceptions.NoBalanceInAccountException;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.ExchangeException;

//TODO refactor not as useful positionId and awkward longPosition/shortPosition handling
public class SimExchange implements Exchange {

    private static final Logger logger = LogManager.getLogger();

    @SuppressWarnings("unused")
    private static final boolean IS_HEDGE_MODE = true;

    private static final double OPEN_LOSS_SIM_MULT = 1.06; // will probably never be this high

    public static final double TAKER_OPEN_CLOSE_FEE = 0.0004;
    public static final double MAKER_OPEN_CLOSE_FEE = 0.0002;

    private SimAccountInfo accInfo;

    private long fundingIntervalMillis;
    private long nextFundingTime;
    private short defaultLeverage;
    private long updateIntervalMillis;

    private HistoricKlines bpHistoricKlines;
    private HistoricFundingRates bpHistoricFundingRates;
    private SlippageModel slippageModel;

    private List<BatchedOrder> batchedMarketOpenOrders = new ArrayList<>();
    private List<BatchedOrder> batchedMarketCloseOrders = new ArrayList<>();

    public SimExchange(double walletBalance, short defaultLeverage, Interval updateInterval,
            HistoricKlines bpHistoricKlines, HistoricFundingRates bpHistoricFundingRates, SlippageModel slippageModel) {

        accInfo = new SimAccountInfo(walletBalance);
        this.defaultLeverage = defaultLeverage;
        updateIntervalMillis = updateInterval.toMilliseconds();
        this.bpHistoricKlines = bpHistoricKlines;
        this.bpHistoricFundingRates = bpHistoricFundingRates;
        fundingIntervalMillis = bpHistoricFundingRates.getFundingIntervalMillis();
        this.slippageModel = slippageModel;
    }

    @Override
    public AccountInfo getAccountInfo(long timestamp) throws ExchangeException {

        try {
            updateAccountInfo(timestamp);
        } catch (NoBalanceInAccountException e) {
            throw new ExchangeException(ExchangeException.ErrorType.INVALID_STATE,
                    "no balance in account, can't continue backtest", "SimExchange::getAccountInfo", e);
        }

        return accInfo;
    }

    private void updateAccountInfo(long timestamp) throws NoBalanceInAccountException {

        // TODO random position adl close, simulate not being able to trade for a period
        // of time
        if (timestamp <= accInfo.lastUpdatedTime) {
            throw new IllegalArgumentException("timestamp must be larger than the previous");
        }

        if (accInfo.lastUpdatedTime != 0) {

            for (long time = accInfo.lastUpdatedTime
                    + updateIntervalMillis; time <= timestamp; time += updateIntervalMillis) {

                calculateMarginBalanceAndUpdatePositionData(time);
                checkMaintenanceMargin(time);

                if (time >= nextFundingTime) {
                    nextFundingTime += fundingIntervalMillis;
                }
            }
            accInfo.nextFundingTime = nextFundingTime;
            accInfo.lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
        } else {
            nextFundingTime = ((timestamp / fundingIntervalMillis) * fundingIntervalMillis) + fundingIntervalMillis;
            accInfo.nextFundingTime = nextFundingTime;
            calculateMarginBalanceAndUpdatePositionData(timestamp);
            checkMaintenanceMargin(timestamp);

            accInfo.lastUpdatedTime = (timestamp / updateIntervalMillis) * updateIntervalMillis;
        }

    }

    // TODO refactor
    private void calculateMarginBalanceAndUpdatePositionData(long timestamp) {

        accInfo.marginBalance = accInfo.walletBalance;
        accInfo.worstCurrentMarginBalance = accInfo.walletBalance;

        for (Map.Entry<String, SimAccountInfo.PositionData> entry : accInfo.longPositions.entrySet()) {
            // should use mark price for more accuracy
            Kline kline = bpHistoricKlines.getKline(entry.getValue().symbol, timestamp);
            double currPrice = kline.getOpen();
            entry.getValue().currPrice = currPrice;
            double pnl = (entry.getValue().currPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
            accInfo.marginBalance += pnl;
            double worstPrice = kline.getLow();
            double worstPnL = (worstPrice - entry.getValue().avgOpenPrice) * entry.getValue().quantity;
            accInfo.worstCurrentMarginBalance += worstPnL;
            int leverage = accInfo.leverages.containsKey(entry.getValue().symbol)
                    ? accInfo.leverages.get(entry.getValue().symbol)
                    : defaultLeverage;
            entry.getValue().margin = (entry.getValue().currPrice * entry.getValue().quantity) / leverage;
        }
        for (Map.Entry<String, SimAccountInfo.PositionData> entry : accInfo.shortPositions.entrySet()) {
            Kline kline = bpHistoricKlines.getKline(entry.getValue().symbol, timestamp);
            double currPrice = kline.getOpen();
            entry.getValue().currPrice = currPrice;
            double pnl = (entry.getValue().avgOpenPrice - entry.getValue().currPrice) * entry.getValue().quantity;
            accInfo.marginBalance += pnl;
            double worstPrice = kline.getHigh();
            double worstPnL = (entry.getValue().avgOpenPrice - worstPrice) * entry.getValue().quantity;
            accInfo.worstCurrentMarginBalance += worstPnL;
            int leverage = accInfo.leverages.containsKey(entry.getValue().symbol)
                    ? accInfo.leverages.get(entry.getValue().symbol)
                    : defaultLeverage;
            entry.getValue().margin = (entry.getValue().currPrice * entry.getValue().quantity) / leverage;
        }
        if (timestamp >= nextFundingTime) {
            for (Map.Entry<String, SimAccountInfo.PositionData> entry : accInfo.longPositions.entrySet()) {
                double fundingRate = bpHistoricFundingRates.getFundingRate(entry.getValue().symbol, timestamp)
                        .getFundingRate();
                accInfo.fundingRates.put(entry.getValue().symbol, fundingRate);
                double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                accInfo.marginBalance -= funding;
                accInfo.walletBalance -= funding;
                accInfo.worstCurrentMarginBalance -= funding;
            }
            for (Map.Entry<String, SimAccountInfo.PositionData> entry : accInfo.longPositions.entrySet()) {
                double fundingRate = bpHistoricFundingRates.getFundingRate(entry.getValue().symbol, timestamp)
                        .getFundingRate();
                accInfo.fundingRates.put(entry.getValue().symbol, fundingRate);
                double funding = entry.getValue().currPrice * entry.getValue().quantity * fundingRate;
                accInfo.marginBalance += funding;
                accInfo.walletBalance += funding;
                accInfo.worstCurrentMarginBalance += funding;
            }
        }
    }

    void checkMaintenanceMargin(long time) throws NoBalanceInAccountException {

        // assuming crossed margin type
        double highestInitialMargin = 0;

        for (Map.Entry<String, SimAccountInfo.PositionData> entry : accInfo.longPositions.entrySet()) {
            highestInitialMargin = Math.max(highestInitialMargin, entry.getValue().margin);
        }
        for (Map.Entry<String, SimAccountInfo.PositionData> entry : accInfo.shortPositions.entrySet()) {
            highestInitialMargin = Math.max(highestInitialMargin, entry.getValue().margin);
        }

        // the maintenance margin is *always less* than 50% of the initial margin
        if (accInfo.worstCurrentMarginBalance <= highestInitialMargin / 2) {
            logger.error("Going to get liquidated at timestamp: {}", time);
            accInfo.marginBalance = 0;
            accInfo.walletBalance = 0;
            accInfo.longPositions = new HashMap<>();
            accInfo.shortPositions = new HashMap<>();
            throw new NoBalanceInAccountException("got liquidated at timestamp: " + time);
        }
    }

    @Override
    public void addBatchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty) {

        batchedMarketOpenOrders.add(new BatchedOrder(orderId, symbol, isLong, symbolQty, true));
    }

    @Override
    public void addBatchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose) {

        batchedMarketCloseOrders.add(new BatchedOrder(orderId, symbol, isLong, qtyToClose, false));
    }

    @Override
    public AccountInfo executeBatchedMarketOpenOrders() {

        ArrayList<OrderError> errors = new ArrayList<>();

        for (BatchedOrder order : batchedMarketOpenOrders) {
            boolean res = marketOpen(order.symbol, order.isLong, order.quantity);
            if (!res) {
                errors.add(new OrderError(order.orderId, OrderError.OrderStatus.NOT_ENOUGH_MARGIN_FAILED, null));
            }
        }

        batchedMarketOpenOrders = new ArrayList<>();

        accInfo.errors = errors;
        return accInfo;
    }

    @Override
    public AccountInfo executeBatchedMarketCloseOrders() {
        ArrayList<OrderError> errors = new ArrayList<>();

        for (BatchedOrder order : batchedMarketCloseOrders) {
            boolean res = marketClose(order.symbol, order.isLong, order.quantity);
            if (!res) {
                errors.add(new OrderError(order.orderId, OrderError.OrderStatus.NO_SUCH_POSITION_FAILED, null));
            }
        }

        batchedMarketCloseOrders = new ArrayList<>();

        accInfo.errors = errors;
        return accInfo;
    }

    // TODO refactor
    private boolean marketOpen(String symbol, boolean isLong, double symbolQty) {

        Kline kline = bpHistoricKlines.getKline(symbol, accInfo.lastUpdatedTime);
        double openPrice = kline.getOpen();
        double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol) : defaultLeverage;
        double initialMargin = (openPrice * symbolQty) / leverage;
        if (isLong) {
            openPrice *= slippageModel.getSlippage(openPrice * symbolQty, symbol);
        } else {
            openPrice *= 1 - (slippageModel.getSlippage(openPrice * symbolQty, symbol) - 1);
        }
        double notionalValue = openPrice * symbolQty;

        double marginUsed = accInfo.getTotalInitialMargin();

        if (marginUsed + (initialMargin * OPEN_LOSS_SIM_MULT) >= accInfo.marginBalance) {
            logger.info("not enough margin to open position at timestamp: {}", accInfo.lastUpdatedTime);
            // TODO a SimLog with successful/unsuccessful opens/closes (not enough margin,
            // log not ok open)
            return false;
        }

        double openFee = notionalValue * TAKER_OPEN_CLOSE_FEE;
        accInfo.marginBalance -= openFee;
        accInfo.walletBalance -= openFee;

        if (isLong) {
            if (accInfo.longPositions.containsKey(symbol)) {
                updatePositionsAfterOpen(accInfo.longPositions, symbol, symbolQty, openPrice, initialMargin);
            } else {
                accInfo.longPositions.put(symbol,
                        accInfo.new PositionData(symbol, isLong, openPrice, symbolQty, initialMargin));
            }
        } else {
            if (accInfo.shortPositions.containsKey(symbol)) {
                updatePositionsAfterOpen(accInfo.shortPositions, symbol, symbolQty, openPrice, initialMargin);
            } else {
                accInfo.shortPositions.put(symbol,
                        accInfo.new PositionData(symbol, isLong, openPrice, symbolQty, initialMargin));
            }
        }

        // TODO a SimLog with successful/unsuccessful opens/closes (log ok open)
        return true;
    }

    private void updatePositionsAfterOpen(HashMap<String, SimAccountInfo.PositionData> positions, String symbol,
            double symbolQty, double openPrice, double initialMargin) {
        positions.get(symbol).margin += initialMargin;
        double newQty = positions.get(symbol).quantity + symbolQty;
        double percOldQty = positions.get(symbol).quantity / newQty;
        double prevAvgOpenPrice = positions.get(symbol).avgOpenPrice;
        double percNewQty = symbolQty / newQty;
        positions.get(symbol).avgOpenPrice = (prevAvgOpenPrice * percOldQty) + (openPrice * percNewQty);
        positions.get(symbol).quantity = newQty;
    }

    // TODO refactor
    private boolean marketClose(String symbol, boolean isLong, double qtyToClose) {
        HashMap<String, SimAccountInfo.PositionData> positions = isLong ? accInfo.longPositions
                : accInfo.shortPositions;
        if (positions.containsKey(symbol)) {

            if (positions.get(symbol).quantity > qtyToClose) {

                double nValSlipp = qtyToClose * positions.get(symbol).currPrice;
                double closePrice;
                double pnl;
                if (positions.get(symbol).isLong) {
                    closePrice = positions.get(symbol).currPrice
                            * (1 - (slippageModel.getSlippage(nValSlipp, symbol) - 1));
                    pnl = (closePrice - positions.get(symbol).avgOpenPrice) * qtyToClose;
                } else {
                    closePrice = positions.get(symbol).currPrice * slippageModel.getSlippage(nValSlipp, symbol);
                    pnl = (positions.get(symbol).avgOpenPrice - closePrice) * qtyToClose;
                }
                double notionalValue = closePrice * qtyToClose;
                double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;

                accInfo.walletBalance -= closeFee;
                accInfo.marginBalance -= closeFee;
                accInfo.walletBalance += pnl;

                double leverage = accInfo.leverages.containsKey(symbol) ? accInfo.leverages.get(symbol)
                        : defaultLeverage;
                double margin = notionalValue / leverage;
                positions.get(symbol).margin -= margin;
                positions.get(symbol).quantity -= qtyToClose;

                // TODO a SimLog with successful/unsuccessful opens/closes (log ok close)
            } else {
                double quantity = positions.get(symbol).quantity;
                double nValSlipp = quantity * positions.get(symbol).currPrice;
                double closePrice;
                double pnl;
                if (positions.get(symbol).isLong) {
                    closePrice = positions.get(symbol).currPrice
                            * (1 - (slippageModel.getSlippage(nValSlipp, symbol) - 1));
                    pnl = (closePrice - positions.get(symbol).avgOpenPrice) * quantity;
                } else {
                    closePrice = positions.get(symbol).currPrice * slippageModel.getSlippage(nValSlipp, symbol);
                    pnl = (positions.get(symbol).avgOpenPrice - closePrice) * quantity;
                }
                double notionalValue = closePrice * quantity;
                double closeFee = Math.abs(notionalValue + pnl) * MAKER_OPEN_CLOSE_FEE;

                accInfo.walletBalance -= closeFee;
                accInfo.marginBalance -= closeFee;
                accInfo.walletBalance += pnl;

                positions.remove(symbol);

                // TODO a SimLog with successful/unsuccessful opens/closes (log ok close)
            }
        } else {
            // TODO a SimLog with successful/unsuccessful opens/closes (no position with
            // this symbol, log not ok close)
            return false;
        }
        return true;
    }

    @Override
    public void setHedgeMode() {

        // only hedge mode supported so it's already in hedge mode
    }

    @Override
    public void setLeverage(String symbol, int leverage) {

        accInfo.leverages.put(symbol, leverage);
    }

    @Override
    public void setCrossMargin(String symbol) {

        // only cross margin supported so all symbols are already in cross margin mode
    }
}
