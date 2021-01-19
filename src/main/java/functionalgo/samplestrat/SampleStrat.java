package functionalgo.samplestrat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import functionalgo.Database;
import functionalgo.Logger;
import functionalgo.binanceperpetual.dataprovider.BPBacktestDataProvider;
import functionalgo.binanceperpetual.dataprovider.BPDataProvider;
import functionalgo.binanceperpetual.dataprovider.BPLiveDataProvider;
import functionalgo.binanceperpetual.exchange.BPAccount;
import functionalgo.binanceperpetual.exchange.BPExchange;
import functionalgo.binanceperpetual.exchange.BPLiveExchange;
import functionalgo.binanceperpetual.exchange.BPSimExchange;
import functionalgo.datapoints.Interval;
import functionalgo.exceptions.ExchangeException;

public class SampleStrat {
    // TODO
    
    private static final String PRIVATE_KEY = "***REMOVED***";
    private static final String API_KEY = "***REMOVED***";
    
    public static final double BACKTEST_START_BALANCE = 100;
    
    public static final Interval INTERVAL = Interval._5m;
    
    boolean isLive;
    
    private BPExchange exchange;
    private BPDataProvider dataProvider;
    
    Logger logger;
    Database database;
    
    class Statistics {
        
        double worstCurrentMarginBalance;
        double marginBalance;
        double walletBalance;
        int wins;
        int losses;
        long timestamp;
        
        public Statistics(double worstCurrentMarginBalance, double marginBalance, double walletBalance, int wins, int losses,
                long timestamp) {
            
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
    }
    
    List<Position> positions;
    
    public SampleStrat(Database database, Logger logger, boolean isLive) throws ExchangeException {
        
        this.isLive = isLive;
        this.logger = logger;
        
        if (isLive) {
            exchange = new BPLiveExchange(logger, PRIVATE_KEY, API_KEY);
            dataProvider = new BPLiveDataProvider(database, logger);
            if (!database.containsTable("SampleStratPositions")) {
                database.createTable("SampleStratPositions");
            }
        } else {
            exchange = new BPSimExchange(BACKTEST_START_BALANCE, (short) 20, Interval._5m);
            dataProvider = new BPBacktestDataProvider(new Interval[] { Interval._5m });
            
            // TODO backtest positions...
            if (!database.containsTable("SampleStratPositions")) {
                database.createTable("SampleStratPositions");
            }
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
                long openTime;
                
                openTime = dataProvider.getKlines(pos.symbol, Interval._5m, adjustedTimestamp, adjustedTimestamp).get(0)
                        .getOpenTime();
                
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
                logger.log(2, -1, e.toString(), Arrays.toString(e.getStackTrace()));
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
                exchange.batchMarketOpen(newPos.symbol + System.currentTimeMillis(), newPos.symbol, newPos.isLong, newPos.qty);
                positions.add(newPos);
            }
            
            acc = exchange.executeBatchedOrders();
            logger.log(1, 0, "SampleStrat:execute:executeBatchedOrders", "string with all batched orders");
            
            for (int i = 0; i < 3; i++) {
                String id = i + "blahblah";
                ExchangeException error = acc.getOrderError(id);
                if (error != null) {
                    // there was an error with that order, handle it. Shouldn't have altered positions before checking for errors
                    logger.log(3, -1, error.toString(), Arrays.toString(error.getStackTrace()));
                }
            }
            
        } catch (ExchangeException e) {
            logger.log(2, -1, e.toString(), Arrays.toString(e.getStackTrace()));
        }
        
        savePositions();
        
        appendStatistics(new Statistics(acc.getWorstCurrenttMarginBalance(), acc.getMarginBalance(), acc.getWalletBalance(), wins,
                losses, acc.getTimestamp()));
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
