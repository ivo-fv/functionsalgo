package functionsalgo.binanceperpetual.exchange;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.binanceperpetual.AccountInfoWrapper;
import functionsalgo.binanceperpetual.ExchangeInfoWrapper;
import functionsalgo.binanceperpetual.OrderResultWrapper;
import functionsalgo.binanceperpetual.WrapperREST;
import functionsalgo.binanceperpetual.WrapperRESTException;
import functionsalgo.binanceperpetual.exchange.exceptions.OrderExecutionException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolNotTradingException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolQuantityTooLow;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.shared.Utils;

public class LiveExchange implements Exchange {

    private static final Logger logger = LogManager.getLogger();

    private boolean hasCalledInfo = false;
    private ExchangeInfoWrapper exchangeInfo;

    private ArrayList<BatchedOrder> batchedMarketOpenOrders = new ArrayList<>();
    private ArrayList<BatchedOrder> batchedMarketCloseOrders = new ArrayList<>();

    WrapperREST api;

    public LiveExchange(String privateKey, String apiKey) throws ExchangeException {
        try {
            api = new WrapperREST(privateKey, apiKey);
        } catch (Exception e) {
            throw new ExchangeException(ExchangeException.ErrorType.INIT_FAILED, "Failed to initialize keys",
                    "LiveExchange::LiveExchange", e);
        }
    }

    @Override
    public AccountInfo getAccountInfo(long timestamp) throws WrapperRESTException {

        LiveAccountInfo acc = new LiveAccountInfo();

        exchangeInfo = api.getExchangeInfo();
        acc.exchangeTime = exchangeInfo.getExchangeTime();

        AccountInfoWrapper wrap = api.getAccountInfo();

        acc.totalInitialMargin = wrap.getTotalInitialMargin();
        acc.marginBalance = wrap.getMarginBalance();
        acc.walletBalance = wrap.getWalletBalance();
        acc.leverages = wrap.getLeverages();
        acc.longPositions = wrap.getLongPositions();
        acc.shortPositions = wrap.getShortPositions();
        acc.isHedgeMode = wrap.isHedgeMode();

        hasCalledInfo = true;

        return acc;
    }

    @Override
    public void setHedgeMode() throws WrapperRESTException {
        api.setToHedgeMode();
    }

    @Override
    public void setLeverage(String symbol, int leverage) throws WrapperRESTException {
        api.setLeverage(symbol, leverage);
    }

    @Override
    public void setCrossMargin(String symbol) throws WrapperRESTException {
        api.setToCrossMargin(symbol);
    }

    @Override
    public void addBatchMarketOpen(int orderId, String symbol, boolean isLong, double symbolQty)
            throws SymbolQuantityTooLow, SymbolNotTradingException {
        if (!hasCalledInfo) {
            throw new IllegalStateException(
                    "Must successfully call getAccountInfo() at least once per LiveExchange instance.");
        }

        if (!exchangeInfo.getIsSymbolTrading(symbol)) {
            throw new SymbolNotTradingException(symbol);
        }

        double stepSize = exchangeInfo.getSymbolQtyStepSize(symbol);
        if (stepSize > symbolQty) {
            throw new SymbolQuantityTooLow(symbol, symbolQty, stepSize);
        }
        double qty = Utils.trimDec(symbolQty, BigDecimal.valueOf(stepSize));
        batchedMarketOpenOrders.add(new BatchedOrder(orderId, symbol, isLong, qty, true));
    }

    @Override
    public void addBatchMarketClose(int orderId, String symbol, boolean isLong, double qtyToClose)
            throws SymbolNotTradingException, SymbolQuantityTooLow {
        if (!hasCalledInfo) {
            throw new IllegalStateException(
                    "Must successfully call getAccountInfo() at least once per LiveExchange instance.");
        }

        if (!exchangeInfo.getIsSymbolTrading(symbol)) {
            throw new SymbolNotTradingException(symbol);
        }

        double stepSize = exchangeInfo.getSymbolQtyStepSize(symbol);
        if (stepSize > qtyToClose) {
            throw new SymbolQuantityTooLow(symbol, qtyToClose, stepSize);
        }
        double qty = Utils.trimDec(qtyToClose, BigDecimal.valueOf(stepSize));
        batchedMarketCloseOrders.add(new BatchedOrder(orderId, symbol, isLong, qty, true));
    }

    @Override
    public AccountInfo executeBatchedMarketOpenOrders() throws OrderExecutionException {

        ArrayList<OrderError> errors = new ArrayList<>();

        for (BatchedOrder order : batchedMarketOpenOrders) {
            try {
                OrderResultWrapper res = api.marketOpenHedgeMode(order.symbol, order.isLong, order.quantity);
                if (!res.getSymbol().equals(order.symbol)) {
                    throw new WrapperRESTException(WrapperRESTException.ErrorType.INCONSISTENT_ORDER_RESULT,
                            "Result symbol doesn't match order symbol", "LiveExchange::executeBatchedMarketOpenOrders");
                }
            } catch (WrapperRESTException e) {
                logger.error("executeBatchedMarketOpenOrders - marketOpenHedgeMode", e);
                logger.error("executeBatchedMarketOpenOrders - marketOpenHedgeMode - {} {} {} {}", order.orderId,
                        order.symbol, order.isLong, order.quantity);

                OrderError.OrderStatus status = OrderError.OrderStatus.FAILED; // assume exchange returned a code
                if (e.getErrorType() != null) {
                    status = OrderError.OrderStatus.UNKNOWN; // exchange didn't return a known code
                }
                errors.add(new OrderError(order.orderId, status, e));
            }
        }

        batchedMarketOpenOrders = new ArrayList<>();

        try {
            LiveAccountInfo acc = (LiveAccountInfo) getAccountInfo(System.currentTimeMillis());
            acc.errors = errors;
            return acc;

        } catch (WrapperRESTException e) {
            throw new OrderExecutionException(errors, e);
        }
    }

    @Override
    public AccountInfo executeBatchedMarketCloseOrders() throws OrderExecutionException {

        ArrayList<OrderError> errors = new ArrayList<>();

        for (BatchedOrder order : batchedMarketCloseOrders) {
            try {
                OrderResultWrapper res = api.marketCloseHedgeMode(order.symbol, order.isLong, order.quantity);
                if (!res.getSymbol().equals(order.symbol)) {
                    throw new WrapperRESTException(WrapperRESTException.ErrorType.INCONSISTENT_ORDER_RESULT,
                            "Result symbol doesn't match order symbol",
                            "LiveExchange::executeBatchedMarketCloseOrders");
                }
            } catch (WrapperRESTException e) {
                logger.error("executeBatchedMarketCloseOrders - marketCloseHedgeMode", e);
                logger.error("executeBatchedMarketCloseOrders - marketCloseHedgeMode - {} {} {} {}", order.orderId,
                        order.symbol, order.isLong, order.quantity);

                OrderError.OrderStatus status = OrderError.OrderStatus.FAILED; // assume exchange returned a code
                if (e.getErrorType() != null) {
                    status = OrderError.OrderStatus.UNKNOWN; // exchange didn't return a known code
                }
                errors.add(new OrderError(order.orderId, status, e));
            }
        }

        batchedMarketCloseOrders = new ArrayList<>();

        try {
            LiveAccountInfo acc = (LiveAccountInfo) getAccountInfo(System.currentTimeMillis());
            acc.errors = errors;
            return acc;

        } catch (WrapperRESTException e) {
            throw new OrderExecutionException(errors, e);
        }
    }

}
