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
    
    public static List<Kline> mergeKlines(Interval interval, List<Kline> _1minKlinesToMerge) {
        
        List<Kline> returnKlines = new ArrayList<>();
        
        int numKlinesToMerge = (int) (interval.toMilliseconds() / Interval._1m.toMilliseconds());
        
        for (int i = 0; i < _1minKlinesToMerge.size(); i++) {
            long openTime = _1minKlinesToMerge.get(i).getOpenTime();
            double open = _1minKlinesToMerge.get(i).getOpen();
            double high = _1minKlinesToMerge.get(i).getHigh();
            double low = _1minKlinesToMerge.get(i).getLow();
            double close = _1minKlinesToMerge.get(i).getClose();
            double volume = _1minKlinesToMerge.get(i).getVolume();
            long closeTime = _1minKlinesToMerge.get(i).getCloseTime();
            double quoteVolume = _1minKlinesToMerge.get(i).getQuoteVolume();
            int numTrades = _1minKlinesToMerge.get(i).getNumTrades();
            double buyVolume = _1minKlinesToMerge.get(i).getBuyVolume();
            double quoteBuyVolume = _1minKlinesToMerge.get(i).getQuoteBuyVolume();
            for (int j = 1; j < numKlinesToMerge; j++) {
                i++;
                if (i > _1minKlinesToMerge.size()) {
                    break;
                }
                high = Math.max(high, _1minKlinesToMerge.get(i).getHigh());
                low = Math.min(low, _1minKlinesToMerge.get(i).getLow());
                close = _1minKlinesToMerge.get(i).getClose();
                volume += _1minKlinesToMerge.get(i).getVolume();
                closeTime = _1minKlinesToMerge.get(i).getCloseTime();
                quoteVolume += _1minKlinesToMerge.get(i).getQuoteVolume();
                numTrades += _1minKlinesToMerge.get(i).getNumTrades();
                buyVolume += _1minKlinesToMerge.get(i).getBuyVolume();
                quoteBuyVolume += _1minKlinesToMerge.get(i).getQuoteBuyVolume();
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
