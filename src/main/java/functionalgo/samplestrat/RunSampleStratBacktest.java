package functionalgo.samplestrat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;

import functionalgo.exceptions.ExchangeException;
import functionalgo.samplestrat.SampleStrat.Statistics;

public class RunSampleStratBacktest {
    
    private class BacktestResult {
        
        int totalTrades = 0;
        int numLosses = 0;
        double currentStratMarginBalance = SampleStrat.BACKTEST_START_BALANCE;
        double highestStratMarginBalance = 0;
        double walletBalance = 0;
        double maxDrawdown = 0;
        
    }
    
    private static final String PLOTS_DIR = "plots/SampleStrat";
    
    private static final double LIQ_ALERT_PERC = 0.7;
    
    private long startTime;
    private long endTime;
    private static char decimalSeparator = ',';
    private File logFile;
    
    public static void main(String[] args) throws ExchangeException {
        
        ArrayList<Long> timeRangeStart = new ArrayList<Long>();
        // timeRangeStart.add(LocalDateTime.of(2019, 12, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        timeRangeStart.add(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeStart.add(LocalDateTime.of(2020, 2, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeStart.add(LocalDateTime.of(2020, 3, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeStart.add(LocalDateTime.of(2020, 4, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeStart.add(LocalDateTime.of(2020, 5, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeStart.add(LocalDateTime.of(2020, 6, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        ArrayList<Long> timeRangeEnd = new ArrayList<Long>();
        // timeRangeEnd.add(LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        timeRangeEnd.add(LocalDateTime.of(2020, 2, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeEnd.add(LocalDateTime.of(2020, 3, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeEnd.add(LocalDateTime.of(2020, 4, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeEnd.add(LocalDateTime.of(2020, 5, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeEnd.add(LocalDateTime.of(2020, 6, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        // timeRangeEnd.add(LocalDateTime.of(2020, 6, 23, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());
        
        double sumScore = 0;
        int numBacktests = 0;
        double highestMarginBalance = Double.NEGATIVE_INFINITY;
        double drawdownOfHighestMarginBalance = 0;
        double highestDrawdown = 0;
        int liqNum = 0;
        int highestMBBacktest = 0;
        int highestDDBacktest = 0;
        
        for (long start : timeRangeStart) {
            for (long end : timeRangeEnd) {
                if (end > start) {
                    numBacktests++;
                    
                    BacktestResult result = new RunSampleStratBacktest(start, end).runBacktest();
                    
                    System.out.println("#" + numBacktests);
                    System.out.println("From: " + LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.of("UTC"))
                            + "  To: " + LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.of("UTC")));
                    System.out.println("Initial wallet balance: " + SampleStrat.BACKTEST_START_BALANCE);
                    System.out.println("Total trades: " + result.totalTrades);
                    System.out.println("Losses: " + result.numLosses);
                    System.out.println("Final strat margin balance: " + result.currentStratMarginBalance);
                    System.out.println("Final strat wallet balance: " + result.walletBalance);
                    System.out.println("Max drawdown: " + result.maxDrawdown);
                    System.out.println("Highest strat margin balance: " + result.highestStratMarginBalance);
                    System.out.println("Score: " + result.currentStratMarginBalance * (1 - result.maxDrawdown));
                    
                    sumScore += result.currentStratMarginBalance * (1 - result.maxDrawdown);
                    if (result.highestStratMarginBalance > highestMarginBalance) {
                        highestMarginBalance = result.highestStratMarginBalance;
                        drawdownOfHighestMarginBalance = result.maxDrawdown;
                        highestMBBacktest = numBacktests;
                    }
                    if (result.maxDrawdown > highestDrawdown) {
                        highestDrawdown = result.maxDrawdown;
                        highestDDBacktest = numBacktests;
                    }
                    
                    if (result.maxDrawdown >= LIQ_ALERT_PERC) {
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.out.println("!!! PROBABLY GOING TO GET LIQUIDATED !!!");
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        liqNum++;
                    }
                }
            }
        }
        System.out.println("------------------------------------------------------------------------------");
        System.out.println("Average score: " + (sumScore / numBacktests));
        System.out.println("Backtest with the highest margin balance: #" + highestMBBacktest + " with " + highestMarginBalance);
        System.out.println("Drawdown of #" + highestMBBacktest + ": " + drawdownOfHighestMarginBalance);
        System.out.println("Highest drawdown of #" + highestMBBacktest + ": " + drawdownOfHighestMarginBalance);
        System.out.println("Backtest with the highest drawdown: #" + highestDDBacktest + " with " + highestDrawdown);
        System.out.println("Liquidations: " + liqNum);
    }
    
    public RunSampleStratBacktest(long startTime, long endTime) {
        
        this.startTime = startTime;
        this.endTime = endTime;
        File plots = new File(PLOTS_DIR);
        plots.mkdirs();
        // 86400000ms = 24h
        logFile = new File(PLOTS_DIR + "/" + LocalDate.ofEpochDay(startTime / 86400000) + "_"
                + LocalDate.ofEpochDay(endTime / 86400000) + ".csv");
        logFile.delete();
    }
    
    public BacktestResult runBacktest() throws ExchangeException {
        
        SampleStrat strat = new SampleStrat(false);
        
        long interval = SampleStrat.INTERVAL.toMilliseconds();
        
        for (long t = startTime; t < endTime; t += interval) {
            
            strat.execute(t);
        }
        
        BacktestResult result = new BacktestResult();
        
        for (Statistics stat : strat.getStatistics()) {
            
            result.totalTrades += stat.losses + stat.wins;
            result.numLosses += stat.losses;
            result.currentStratMarginBalance += stat.marginBalance;
            result.walletBalance += stat.walletBalance;
            
            if (result.currentStratMarginBalance > result.highestStratMarginBalance) {
                result.highestStratMarginBalance = result.currentStratMarginBalance;
            }
            double drawdown = 1 - (stat.worstCurrentMarginBalance / result.highestStratMarginBalance);
            if (drawdown > result.maxDrawdown) {
                result.maxDrawdown = drawdown;
            }
            
        }
        
        DecimalFormatSymbols decimalSymbols = DecimalFormatSymbols.getInstance();
        decimalSymbols.setDecimalSeparator(decimalSeparator);
        DecimalFormat formatDecimalSeparator = new DecimalFormat("0.0000", decimalSymbols);
        try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logFile, true)))) {
            
            for (Statistics stat : strat.getStatistics()) {
                long timestamp = stat.timestamp;
                double marginBalance = stat.marginBalance;
                out.println(formatDecimalSeparator.format(marginBalance) + ";" + timestamp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
}
