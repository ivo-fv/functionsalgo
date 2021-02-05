package functionsalgo.samplestrat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.binanceperpetual.exchange.AccountInfo;
import functionsalgo.binanceperpetual.exchange.Exchange;
import functionsalgo.binanceperpetual.exchange.LiveExchange;
import functionsalgo.binanceperpetual.exchange.SimExchange;
import functionsalgo.binanceperpetual.exchange.exceptions.OrderExecutionException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolNotTradingException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolQuantityTooLow;
import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Strategy;
import functionsalgo.shared.Utils;

public class SampleStrategy implements Strategy {

    private static final Logger logger = LogManager.getLogger();

    private static final double BP_BACKTEST_INIITIAL_BALANCE = 100;
    private static final short BP_BACKTEST_DEFAULT_LEVERAGE = 2;
    private static final Interval BP_BACKTEST_TICK_INTERVAL = Interval._5m;

    Exchange bpExch;

    StrategyDecision strat = new StrategyDecision();
    boolean live = false;
    int lastOrderId;

    HistoricKlines bpHistoricKlines;
    HistoricFundingRates bpHistoricFundingRates;
    SlippageModel bpSlippageModel;

    public static Strategy getLiveStrategy() throws ExchangeException, StandardJavaException {
        return new SampleStrategy(true);
    }

    public static Strategy getBacktestStrategy(boolean generateBacktestData, String configFileName)
            throws ExchangeException, StandardJavaException {

        Properties backtesterConfig = Utils.getProperties(configFileName, "backtest_config_example.txt");
        String symbolListProp = backtesterConfig.getProperty("binanceperpetual.symbols");
        List<String> symbolList = Arrays.asList(symbolListProp.split(",", 0));
        Interval interval = Interval.parseString(backtesterConfig.getProperty("binanceperpetual.interval"));
        long startTime = Long.valueOf(backtesterConfig.getProperty("binanceperpetual.startTime"));
        long endTime = Long.valueOf(backtesterConfig.getProperty("binanceperpetual.endTime"));

        SampleStrategy strat = new SampleStrategy(false);
        if (generateBacktestData) {
            try {
                strat.bpHistoricKlines = HistoricKlines.pullKlines(symbolList, interval, startTime, endTime);
                strat.bpHistoricFundingRates = null; // TODO
                strat.bpSlippageModel = null; // TODO
            } catch (StandardJavaException e) {
                logger.error("couldn't pull backtest data", e);
            }
        }
        try {
            strat.bpHistoricKlines = HistoricKlines.loadKlines(interval);
            strat.bpHistoricFundingRates = null; // TODO
            strat.bpSlippageModel = null; // TODO
        } catch (StandardJavaException e) {
            logger.error("couldn't load backtest data", e);
        }
        return strat;
    }

    private SampleStrategy(boolean isLive) throws ExchangeException, StandardJavaException {
        this.live = isLive;

        if (isLive) {
            Properties keys = Utils.getProperties("binanceperpetual_apikeys_ignore.properties",
                    "binanceperpetual_apikeys.properties");
            bpExch = new LiveExchange(keys.getProperty("privateKey"), keys.getProperty("publicApiKey"));
        } else {
            bpExch = new SimExchange(BP_BACKTEST_INIITIAL_BALANCE, BP_BACKTEST_DEFAULT_LEVERAGE,
                    BP_BACKTEST_TICK_INTERVAL, bpHistoricKlines, bpHistoricFundingRates, bpSlippageModel); // TODO
        }
    }

    @Override
    public SampleStratTradeStatistics execute(long timestamp) {

        AccountInfo acc = null;
        try {
            acc = bpExch.getAccountInfo(timestamp);
        } catch (ExchangeException e) {
            logger.error("first getAccountInfo failed", e);
            // the algorithm should handle these kinds of problems
        }
        // TODO get current state from DB, use it for getSymbols and to set lastOrderId
        // TODO handle orphaned positions

        List<Position> posToclose = getPositionsToClose(acc);
        try {
            acc = closePositions(posToclose);
            // TODO if present, handle errors present in acc
        } catch (OrderExecutionException e) {
            logger.error("closePositions failed", e);
            // the algorithm should handle these kinds of problems
        }

        List<Position> posToOpen = getPositionsToOpen(acc);
        try {
            acc = openPositions(posToOpen);
            // TODO if present, handle errors present in acc
        } catch (OrderExecutionException e) {
            logger.error("openPositions failed", e);
            // the algorithm should handle these kinds of problems
        }

        // TODO check for orphaned positions

        // TODO save acc and lastOrderId and current state in DB

        // TODO Auto-generated method stub
        return null;
    }

    AccountInfo openPositions(List<Position> posToOpen) throws OrderExecutionException {

        for (Position pos : posToOpen) {
            try {
                bpExch.addBatchMarketOpen(pos.id, pos.symbol, pos.isLong, pos.quantity);
            } catch (SymbolQuantityTooLow | SymbolNotTradingException e) {
                logger.error("openPositions - batchMarketOpen failed", e);
                // the algorithm should handle these kinds of problems
            }
        }
        return bpExch.executeBatchedMarketOpenOrders();
    }

    AccountInfo closePositions(List<Position> posToclose) throws OrderExecutionException {

        for (Position pos : posToclose) {
            try {
                bpExch.addBatchMarketClose(pos.id, pos.symbol, pos.isLong, pos.quantity);
            } catch (SymbolQuantityTooLow | SymbolNotTradingException e) {
                logger.error("closePositions - batchMarketClose failed", e);
                // the algorithm should handle these kinds of problems
            }
        }
        return bpExch.executeBatchedMarketCloseOrders();
    }

    List<Position> getPositionsToOpen(AccountInfo acc) {
        // strat.something()
        // TODO Auto-generated method stub
        return null;
    }

    List<Position> getPositionsToClose(AccountInfo acc) {
        // strat.something()
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLive() {
        return live;
    }

}
