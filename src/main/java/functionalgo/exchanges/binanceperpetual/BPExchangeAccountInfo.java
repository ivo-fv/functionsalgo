package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeAccountInfo;

public interface BPExchangeAccountInfo extends ExchangeAccountInfo {
    
    double getQty(String positionId);
    
    long getLastUpdateTimestamp();
    
    double getWalletBalance();
    
    double getInitialMargin(String positionId);
    
    double getPnL(String positionId);
    
    int getLeverage(String symbol);
    
    double getTotalFundingFees(String positionId);
    
    double getTakerFee();
}
