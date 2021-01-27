package functionsalgo.samplestrat;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.aws.DynamoDBBPDataProvider;
import functionsalgo.aws.DynamoDBCommon;
import functionsalgo.aws.DynamoDBSampleStrat;
import functionsalgo.aws.SampleStratDB;
import functionsalgo.binanceperpetual.BPWrapperREST;
import functionsalgo.binanceperpetual.dataprovider.BPBacktestDataProvider;
import functionsalgo.binanceperpetual.dataprovider.BPDataProvider;
import functionsalgo.binanceperpetual.dataprovider.BPLiveDataProvider;
import functionsalgo.binanceperpetual.exchange.BPAccount;
import functionsalgo.binanceperpetual.exchange.BPExchange;
import functionsalgo.binanceperpetual.exchange.BPLiveExchange;
import functionsalgo.binanceperpetual.exchange.BPSimExchange;
import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.ExchangeException;

public class SampleStrat {
    // TODO

    // TODO use credentials/key manager
    // currently just test keys of dummy test account
    private static final String PRIVATE_KEY = "b1de68c44b95077fa829d9a904b84c8edc89405ca0ae0f1768cbbdb9cabf841b";
    private static final String API_KEY = "a02d4409583be65a2721e2de10104e1e6232f402d1fd909cd9390e4aa17aefad";

    public static final double BACKTEST_START_BALANCE = 100;

    public static final Interval INTERVAL = Interval._5m;

    private static final Logger logger = LogManager.getLogger();

    boolean isLive;

    private BPExchange exchange;
    private BPDataProvider dataProvider;

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
            return "Position [symbol=" + symbol + ", quantity=" + qty + ", isLong=" + isLong + "]";
        }

    }

    List<Position> positions;

    public SampleStrat(boolean isLive, boolean isTest)
            throws ExchangeException, InvalidKeyException, NoSuchAlgorithmException {

        this.isLive = isLive;

        if (isLive) {
            DynamoDBCommon dbCommon = new DynamoDBCommon();
            database = new DynamoDBSampleStrat(dbCommon);
            BPWrapperREST apiHandler = new BPWrapperREST(PRIVATE_KEY, API_KEY, false);
            exchange = new BPLiveExchange(apiHandler, PRIVATE_KEY, API_KEY);
            dataProvider = new BPLiveDataProvider(new DynamoDBBPDataProvider(dbCommon), apiHandler);
        } else {
            database = new SampleStratBacktestDB();
            exchange = new BPSimExchange(BACKTEST_START_BALANCE, (short) 20, Interval._5m);
            dataProvider = new BPBacktestDataProvider(new Interval[] { Interval._5m });
        }
    }

    public void execute(long timestamp) throws ExchangeException {

        // TODO test some printlns

        positions = getPositions();

        BPAccount acc = exchange.getAccountInfo(timestamp);

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
                    exchange.batchMarketClose(pos.symbol + System.currentTimeMillis(), pos.symbol, pos.isLong, pos.qty);
                    posToClose.add(pos);
                    if (adjustedTimestamp % 2 == 0) {
                        wins++;
                    } else {
                        losses++;
                    }
                }

            } catch (ExchangeException e) {
                logger.error("when batchMarketClose - Position: " + pos.toString() + " | posToClose: "
                        + posToClose.toString(), e);
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
                exchange.batchMarketOpen(newPos.symbol + System.currentTimeMillis(), newPos.symbol, newPos.isLong,
                        newPos.qty);
                positions.add(newPos);
            }

            acc = exchange.executeBatchedOrders();
            logger.warn("execute - executeBatchedOrders - log a string with all batched orders");

            for (int i = 0; i < 3; i++) {
                String id = i + "blahblah";
                ExchangeException error = acc.getOrderError(id);
                if (error != null) {
                    // there was an error with that order, handle it. Shouldn't have altered
                    // positions before checking for errors
                    logger.warn("there was an order with an error - ", error);
                }
            }

        } catch (ExchangeException e) {
            logger.error("batchMarketOpen or executeBatchedOrders", e);
        }

        savePositions();

        appendStatistics(new Statistics(acc.getWorstCurrenttMarginBalance(), acc.getMarginBalance(),
                acc.getWalletBalance(), wins, losses, acc.getTimestamp()));
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

}
