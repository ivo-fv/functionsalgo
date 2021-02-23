package functionsalgo.samplestrat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.binanceperpetual.dataprovider.BacktestDataProvider;
import functionsalgo.binanceperpetual.dataprovider.DataProvider;
import functionsalgo.binanceperpetual.exchange.AccountInfo;
import functionsalgo.binanceperpetual.exchange.Exchange;
import functionsalgo.binanceperpetual.exchange.LiveExchange;
import functionsalgo.binanceperpetual.exchange.SimExchange;
import functionsalgo.binanceperpetual.exchange.exceptions.OrderExecutionException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolNotTradingException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolQuantityTooLow;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Timestamp;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Strategy;
import functionsalgo.shared.Utils;

public class SampleStrategy implements Strategy {

    private static final Logger logger = LogManager.getLogger();

    public static List<String> symbolsToTrade = Arrays.asList("BTCUSDT", "ETHUSDT");

    Exchange bpExch;
    DataProvider bpData;
    Storage storage;
    SampleStrategyStatistics stats = new SampleStrategyStatistics();

    boolean live = false;
    State state;
    Timestamp timestamp;
    Interval interval = Interval._5m;
    int id = 0;
    double riskPer = 0.05;
    AccountInfo acc;
    int leverage = 10;

    static HistoricKlines bpHistoricKlines;
    static HistoricFundingRates bpHistoricFundingRates;
    static SlippageModel bpSlippageModel;

    public static Strategy getLiveStrategy() throws ExchangeException, IOException {
        return new SampleStrategy();
    }

    public static Strategy getBacktestStrategy(BacktestConfiguration config) throws StandardJavaException {
        return new SampleStrategy(config);
    }

    private SampleStrategy() throws ExchangeException, IOException {
        live = true;
        Properties keys = new Properties();
        keys.load(Utils.getFileOrResource("secrets_ignore.properties", "secrets.properties").openStream());
        bpExch = new LiveExchange(keys.getProperty("binanceperpetual.privateKey"),
                keys.getProperty("binanceperpetual.publicApiKey"));
        // TODO make sure leverage gets set to this.leverage
        // TODO bpData = new LiveDataProvider(...);
        // TODO storage = LiveStorage(...);
    }

    private SampleStrategy(BacktestConfiguration config) throws StandardJavaException {
        live = false;
        HistoricKlines klines = config.getBPKlines();
        HistoricFundingRates frates = config.getBPFundingRates();
        bpExch = new SimExchange(config.getBPInitialBalance(), config.getBPDefaultLeverage(), klines.getInterval(),
                klines, frates, config.getBPSlippageModel());
        for (String symbol : symbolsToTrade) {
            try {
                bpExch.setLeverage(symbol, leverage);
            } catch (ExchangeException e) {
                // will never happen in a backtest
            }
        }

        Map<Interval, HistoricKlines> klinesPerInterval = new HashMap<>();
        klinesPerInterval.put(klines.getInterval(), klines);
        bpData = new BacktestDataProvider(klinesPerInterval, frates);
        storage = new BacktestStorage(new State());
    }

    @Override
    public boolean isLive() {
        return live;
    }

    @Override
    public SampleStrategyStatistics execute(long timestamp) {
        // arbitrary interval decided by whatever the strategy is
        this.timestamp = new Timestamp(timestamp, Interval._5m);

        try {
            acc = bpExch.getAccountInfo(timestamp);
        } catch (ExchangeException e) {
            logger.error("first getAccountInfo failed", e);
            // the algorithm should handle these kinds of problems
        }

        state = storage.getCurrentState();
        // TODO for live: stats = storage.getStats();

        syncState(state, acc);

        List<Position> posToclose = getPositionsToClose();
        try {
            acc = closePositions(posToclose);
            // TODO (live) if present, handle errors present in acc (includes removing non
            // handled bad positions from state via orderId using acc.getOrderErrors())
        } catch (OrderExecutionException e) {
            logger.error("closePositions failed", e);
            // (live) the algorithm should handle these kinds of problems, call syncState
            // with a fresh account
        }

        List<Position> posToOpen = getPositionsToOpen();
        try {
            acc = openPositions(posToOpen);
            // TODO (live) if present, handle errors present in acc (includes removing non
            // handled bad positions from state via orderId using acc.getOrderErrors())
        } catch (OrderExecutionException e) {
            logger.error("openPositions failed", e);
            // (live) the algorithm should handle these kinds of problems, call syncState
            // with a fresh account
        }

        stats.addBalance(acc.getTimestampMillis(), acc.getMarginBalance());
        stats.addWalletBalance(acc.getTimestampMillis(), acc.getWalletBalance());

        syncState(state, acc);

        try {
            storage.saveCurrentState(state);
        } catch (IOException e) {
            logger.error("couldn't save state");
            // TODO (live) handle this
        }

        return stats;
    }

    private void syncState(State state, AccountInfo acc) {
        // handleOrphanedAndInconsistentPositions, for live trading
        // TODO Auto-generated method stub
    }

    AccountInfo openPositions(List<Position> posToOpen) throws OrderExecutionException {

        for (Position pos : posToOpen) {
            try {
                bpExch.addBatchMarketOpen(pos.id, pos.symbol, pos.isLong, pos.quantity);
                state.addPosition(pos.id, pos);
            } catch (SymbolQuantityTooLow | SymbolNotTradingException e) {
                logger.error("openPositions - batchMarketOpen failed", e);
                state.remove(pos.id);
                // TODO (live) the algorithm should handle these kinds of problems
            }
        }
        return bpExch.executeBatchedMarketOpenOrders();
    }

    AccountInfo closePositions(List<Position> posToclose) throws OrderExecutionException {

        for (Position pos : posToclose) {
            try {
                bpExch.addBatchMarketClose(pos.id, pos.symbol, pos.isLong, pos.quantity);
                state.remove(pos.id);
            } catch (SymbolQuantityTooLow | SymbolNotTradingException e) {
                logger.error("closePositions - batchMarketClose failed", e);
                state.addPosition(pos.id, pos);
                // TODO (live) the algorithm should handle these kinds of problems
            }
        }
        return bpExch.executeBatchedMarketCloseOrders();
    }

    List<Position> getPositionsToOpen() {
        List<Position> positions = new ArrayList<>();
        for (String symbol : symbolsToTrade) {
            try {
                Timestamp prev12hTime = timestamp.copy().sub(Interval._12h.toMilliseconds());
                double previous12hPrice = bpData.getKlines(symbol, interval, prev12hTime, prev12hTime)
                        .get(prev12hTime.getTime()).getOpen();
                double currPrice = bpData.getKlines(symbol, interval, timestamp, timestamp).get(timestamp.getTime())
                        .getOpen();
                if (currPrice / previous12hPrice >= 1.05) {
                    double qty = acc.getMarginBalance() * riskPer * leverage / currPrice;
                    positions.add(new Position(id++, symbol, true, qty));
                }
            } catch (ExchangeException e) {
                logger.error("couldn't get kline data of {} at {}", symbol, timestamp.getTime());
            }
        }
        return positions;
    }

    List<Position> getPositionsToClose() {
        // TODO for each symbol if avgprice lower than x% Xhours before
        // TODO Auto-generated method stub
        return null;
    }
}
