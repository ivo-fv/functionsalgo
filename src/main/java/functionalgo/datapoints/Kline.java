package functionalgo.datapoints;

import java.io.Serializable;

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
    
}