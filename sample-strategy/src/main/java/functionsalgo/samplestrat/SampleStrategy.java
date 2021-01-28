package functionsalgo.samplestrat;

import java.util.List;

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

public class SampleStrategy implements Strategy {

    // TODO use credentials/key manager
    // currently just test keys of dummy test account
    private static final String PRIVATE_KEY = "b1de68c44b95077fa829d9a904b84c8edc89405ca0ae0f1768cbbdb9cabf841b";
    private static final String API_KEY = "a02d4409583be65a2721e2de10104e1e6232f402d1fd909cd9390e4aa17aefad";

    private static final Logger logger = LogManager.getLogger();

    Exchange bpExch;

    StrategyDecision strat = new StrategyDecision();
    boolean live = false;
    int lastOrderId;

    public SampleStrategy(boolean isLive) throws ExchangeException {
        if (isLive) {
            live = true;
            bpExch = new LiveExchange(PRIVATE_KEY, API_KEY);
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
