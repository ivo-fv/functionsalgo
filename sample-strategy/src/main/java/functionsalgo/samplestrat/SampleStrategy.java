package functionsalgo.samplestrat;

import java.io.IOException;
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
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Strategy;
import functionsalgo.shared.Utils;

//TODO dataproviders, statistics while execute
public class SampleStrategy implements Strategy {

    private static final Logger logger = LogManager.getLogger();

    Exchange bpExch;
    DataProvider bpData;
    Storage storage;
    SampleStrategyStatistics stats = new SampleStrategyStatistics();

    StrategyDecision strat = new StrategyDecision();
    boolean live = false;
    State state;

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
        // TODO bpData = new LiveDataProvider(...);
        // TODO storage = LiveStorage(...);
    }

    private SampleStrategy(BacktestConfiguration config) throws StandardJavaException {
        live = false;
        HistoricKlines klines = config.getBPKlines();
        HistoricFundingRates frates = config.getBPFundingRates();
        bpExch = new SimExchange(config.getBPInitialBalance(), config.getBPDefaultLeverage(), klines.getInterval(),
                klines, frates, config.getBPSlippageModel());
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

        AccountInfo acc = null;
        try {
            acc = bpExch.getAccountInfo(timestamp);
        } catch (ExchangeException e) {
            logger.error("first getAccountInfo failed", e);
            // the algorithm should handle these kinds of problems
        }

        state = storage.getCurrentState();

        syncState(state, acc);

        List<Position> posToclose = getPositionsToClose();
        try {
            acc = closePositions(posToclose);
            // TODO if present, handle errors present in acc (includes removing non handled
            // bad positions from state via orderId)
        } catch (OrderExecutionException e) {
            logger.error("closePositions failed", e);
            // the algorithm should handle these kinds of problems
        }

        List<Position> posToOpen = getPositionsToOpen();
        try {
            acc = openPositions(posToOpen);
            // TODO if present, handle errors present in acc (includes removing non handled
            // bad positions from state via orderId)
        } catch (OrderExecutionException e) {
            logger.error("openPositions failed", e);
            // the algorithm should handle these kinds of problems
        }

        stats.addBalance(acc.getTimestampMillis(), acc.getMarginBalance());
        stats.addWalletBalance(acc.getTimestampMillis(), acc.getWalletBalance());

        syncState(state, acc);

        try {
            storage.saveCurrentState(state);
        } catch (IOException e) {
            logger.error("couldn't save state");
            // TODO maybe handle this
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
                // the algorithm should handle these kinds of problems
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
                // the algorithm should handle these kinds of problems
            }
        }
        return bpExch.executeBatchedMarketCloseOrders();
    }

    List<Position> getPositionsToOpen() {
        // StrategyDecision strat.something(state)
        // TODO Auto-generated method stub
        return null;
    }

    List<Position> getPositionsToClose() {
        // StrategyDecision strat.something(state)
        // TODO Auto-generated method stub
        return null;
    }
}
