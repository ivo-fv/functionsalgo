package functionsalgo.samplestrat;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.aws.DynamoDBBPDataProvider;
import functionsalgo.aws.DynamoDBCommon;
import functionsalgo.aws.DynamoDBSampleStrat;
import functionsalgo.aws.SampleStratDB;
import functionsalgo.binanceperpetual.WrapperREST;
import functionsalgo.binanceperpetual.dataprovider.BacktestDataProvider;
import functionsalgo.binanceperpetual.dataprovider.DataProvider;
import functionsalgo.binanceperpetual.dataprovider.LiveDataProvider;
import functionsalgo.binanceperpetual.exchange.AccountInfo;
import functionsalgo.binanceperpetual.exchange.Exchange;
import functionsalgo.binanceperpetual.exchange.LiveExchange;
import functionsalgo.binanceperpetual.exchange.SimExchange;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolNotTradingException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolQuantityTooLow;
import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Strategy;
import functionsalgo.shared.TradeStatistics;

public class OLDSampleStrat implements Strategy {
    // TODO

    // TODO use credentials/key manager
    // currently just test keys of dummy test account
    private static final String PRIVATE_KEY = "";
    private static final String API_KEY = "";

    public static final double BACKTEST_START_BALANCE = 100;

    public static final Interval INTERVAL = Interval._5m;

    private static final Logger logger = LogManager.getLogger();

    boolean isLive;

    private Exchange exchange;
    private DataProvider dataProvider;

    SampleStratDB database;

    class Statistics {

        double worstCurrentMarginBalance;
        double marginBalance;
        double walletBalance;
        int wins;
        int losses;
        long timestamp;

        public Statistics(double worstCurrentMarginBalance, double marginBalance, double walletBalance, int wins,
                int losses, long timestamp) {

            this.worstCurrentMarginBalance = worstCurrentMarginBalance;
            this.marginBalance = marginBalance;
            this.walletBalance = walletBalance;
            this.wins = wins;
            this.losses = losses;
            this.timestamp = timestamp;
        }
    }

    class Position {

        String symbol;
        double qty;
        boolean isLong;

        Position(String symbol, double qty, boolean isLong) {

            this.symbol = symbol;
            this.qty = qty;
            this.isLong = isLong;
        }

        @Override
        public String toString() {
            return "PositionWrapper [symbol=" + symbol + ", quantity=" + qty + ", isLong=" + isLong + "]";
        }

    }

    List<Position> positions;

    public OLDSampleStrat(boolean isLive, boolean isTest)
            throws ExchangeException, InvalidKeyException, NoSuchAlgorithmException, StandardJavaException {

        this.isLive = isLive;

        if (isLive) {
            DynamoDBCommon dbCommon = new DynamoDBCommon();
            database = new DynamoDBSampleStrat(dbCommon);
            exchange = new LiveExchange(PRIVATE_KEY, API_KEY);
            dataProvider = new LiveDataProvider(new DynamoDBBPDataProvider(dbCommon));
        } else {
            database = new OLDSampleStratBacktestDB();
            exchange = new SimExchange(BACKTEST_START_BALANCE, (short) 20, Interval._5m, null, null, null);
            // dataProvider = new BacktestDataProvider(new Interval[] { Interval._5m });
        }
    }

    @Override
    public TradeStatistics execute(long timestamp) {

        // TODO test some printlns

        positions = getPositions();

        AccountInfo acc = null;
        try {
            acc = exchange.getAccountInfo(timestamp);
        } catch (ExchangeException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        long adjustedTimestamp = (timestamp / Interval._5m.toMilliseconds()) * Interval._5m.toMilliseconds();
        ArrayList<Position> posToClose = new ArrayList<>();

        int wins = 0;
        int losses = 0;

        // use a map to map id to position instead of list in actual strats

        for (Position pos : positions) {

            try {
                long openTime = dataProvider.getKlines(pos.symbol, Interval._5m, adjustedTimestamp, adjustedTimestamp)
                        .get(0).getOpenTime();

                boolean shouldClose = openTime % 2 == 0 ? true : false;

                if (shouldClose) {
                    exchange.addBatchMarketClose(pos.symbol + System.currentTimeMillis(), pos.symbol, pos.isLong,
                            pos.qty);
                    posToClose.add(pos);
                    if (adjustedTimestamp % 2 == 0) {
                        wins++;
                    } else {
                        losses++;
                    }
                }

            } catch (ExchangeException e) {
                logger.error("when batchMarketClose - PositionWrapper: " + pos.toString() + " | posToClose: "
                        + posToClose.toString(), e);
            } catch (SymbolQuantityTooLow e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SymbolNotTradingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        for (Position pos : posToClose) {
            if (positions.contains(pos)) {
                positions.remove(pos);
            }
        }

        try {
            if ((int) Math.floor(acc.getMarginBalance()) % 2 == 0 && acc.getTimestamp() % 2 == 0) {
                Position newPos = new Position("ETHUSDT", 0.5, true);
                exchange.addBatchMarketOpen(newPos.symbol + System.currentTimeMillis(), newPos.symbol, newPos.isLong,
                        newPos.qty);
                positions.add(newPos);
            }

            // acc = exchange.executeBatchedOrders();
            logger.warn("execute - executeBatchedOrders - log a string with all batched orders");

            for (int i = 0; i < 3; i++) {
                String id = i + "blahblah";
                ExchangeException error = null;// acc.getOrderError(id);
                if (error != null) {
                    // there was an error with that order, handle it. Shouldn't have altered
                    // positions before checking for errors
                    logger.warn("there was an order with an error - ", error);
                }
            }

        } catch (SymbolQuantityTooLow e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SymbolNotTradingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        savePositions();

        appendStatistics(new Statistics(acc.getMarginBalance(), acc.getMarginBalance(), acc.getWalletBalance(), wins,
                losses, acc.getTimestamp()));
        return null; // TODO TradeStatistics
    }

    private void appendStatistics(Statistics statistics) {

        // in database append statistics...
        // TODO Auto-generated method stub

    }

    public List<Statistics> getStatistics() {

        // TODO get statistics from database...
        return null;
    }

    private void savePositions() {

        // in database save positions...
        // TODO Auto-generated method stub

    }

    private List<Position> getPositions() {

        // database...
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLive() {
        // TODO Auto-generated method stub
        return false;
    }

}
