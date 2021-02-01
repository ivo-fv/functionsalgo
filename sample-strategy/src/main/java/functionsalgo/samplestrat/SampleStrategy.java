package functionsalgo.samplestrat;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.binanceperpetual.exchange.AccountInfo;
import functionsalgo.binanceperpetual.exchange.Exchange;
import functionsalgo.binanceperpetual.exchange.LiveExchange;
import functionsalgo.binanceperpetual.exchange.exceptions.OrderExecutionException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolNotTradingException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolQuantityTooLow;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.shared.Strategy;
import functionsalgo.shared.Utils;

public class SampleStrategy implements Strategy {

    private static final Logger logger = LogManager.getLogger();

    Exchange bpExch;

    StrategyDecision strat = new StrategyDecision();
    boolean live = false;
    int lastOrderId;

    public SampleStrategy(boolean isLive) throws ExchangeException, ClassNotFoundException, IOException {
        if (isLive) {
            live = true;
            Properties keys = Utils.getProperties("apikeys_ignore.properties", "apikeys.properties");
            bpExch = new LiveExchange(keys.getProperty("privateKey"), keys.getProperty("publicApiKey"));
        } else {
            // TODO simexchange
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
