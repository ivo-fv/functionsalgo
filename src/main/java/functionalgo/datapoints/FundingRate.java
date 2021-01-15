package functionalgo.datapoints;

import java.io.Serializable;

public class FundingRate implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String symbol;
    private double fundingRate;
    private long fundingTime;
    
    public FundingRate(String symbol, double fundingRate, long fundingTime) {
        
        this.symbol = symbol;
        this.fundingRate = fundingRate;
        this.fundingTime = fundingTime;
    }
    
    public static long getSerialversionuid() {
        
        return serialVersionUID;
    }
    
    public String getSymbol() {
        
        return symbol;
    }
    
    public double getFundingRate() {
        
        return fundingRate;
    }
    
    public long getFundingTime() {
        
        return fundingTime;
    }
}
