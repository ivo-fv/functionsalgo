package functionalgo.datapoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Kline implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long openTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private long closeTime;
    private double quoteVolume;
    private int numTrades;
    private double buyVolume;
    private double quoteBuyVolume;
    
    public Kline(long openTime, double open, double high, double low, double close, double volume, long closeTime,
            double quoteVolume, int numTrades, double buyVolume, double quoteBuyVolume) {
        
        this.openTime = openTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.closeTime = closeTime;
        this.quoteVolume = quoteVolume;
        this.numTrades = numTrades;
        this.buyVolume = buyVolume;
        this.quoteBuyVolume = quoteBuyVolume;
    }
    
    public static List<Kline> mergeKlines(Interval inInterval, List<Kline> klinesToMerge, Interval outInterval) {
        
        List<Kline> returnKlines = new ArrayList<>();
        
        int numKlinesToMerge = (int) (outInterval.toMilliseconds() / inInterval.toMilliseconds());
        
        for (int i = 0; i < klinesToMerge.size(); i++) {
            long openTime = klinesToMerge.get(i).getOpenTime();
            double open = klinesToMerge.get(i).getOpen();
            double high = klinesToMerge.get(i).getHigh();
            double low = klinesToMerge.get(i).getLow();
            double close = klinesToMerge.get(i).getClose();
            double volume = klinesToMerge.get(i).getVolume();
            long closeTime = klinesToMerge.get(i).getCloseTime();
            double quoteVolume = klinesToMerge.get(i).getQuoteVolume();
            int numTrades = klinesToMerge.get(i).getNumTrades();
            double buyVolume = klinesToMerge.get(i).getBuyVolume();
            double quoteBuyVolume = klinesToMerge.get(i).getQuoteBuyVolume();
            for (int j = 1; j < numKlinesToMerge; j++) {
                i++;
                if (i > klinesToMerge.size()) {
                    break;
                }
                high = Math.max(high, klinesToMerge.get(i).getHigh());
                low = Math.min(low, klinesToMerge.get(i).getLow());
                close = klinesToMerge.get(i).getClose();
                volume += klinesToMerge.get(i).getVolume();
                closeTime = klinesToMerge.get(i).getCloseTime();
                quoteVolume += klinesToMerge.get(i).getQuoteVolume();
                numTrades += klinesToMerge.get(i).getNumTrades();
                buyVolume += klinesToMerge.get(i).getBuyVolume();
                quoteBuyVolume += klinesToMerge.get(i).getQuoteBuyVolume();
            }
            returnKlines.add(new Kline(openTime, open, high, low, close, volume, closeTime, quoteVolume, numTrades, buyVolume,
                    quoteBuyVolume));
        }
        
        return returnKlines;
    }
    
    public long getOpenTime() {
        
        return openTime;
    }
    
    public double getOpen() {
        
        return open;
    }
    
    public double getHigh() {
        
        return high;
    }
    
    public double getLow() {
        
        return low;
    }
    
    public double getClose() {
        
        return close;
    }
    
    public double getVolume() {
        
        return volume;
    }
    
    public long getCloseTime() {
        
        return closeTime;
    }
    
    public double getQuoteVolume() {
        
        return quoteVolume;
    }
    
    public int getNumTrades() {
        
        return numTrades;
    }
    
    public double getBuyVolume() {
        
        return buyVolume;
    }
    
    public double getQuoteBuyVolume() {
        
        return quoteBuyVolume;
    }
}
