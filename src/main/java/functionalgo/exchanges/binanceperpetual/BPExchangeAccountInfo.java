package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeAccountInfo;

public interface BPExchangeAccountInfo extends ExchangeAccountInfo {
    
    double getQty(String positionId);
    
    long getLastUpdateTimestamp();
    
    double getWalletBalance();
    
    double getInitialMargin(String positionId);
    
    double getMarketClosePnL(String positionId, double qtyToClose);
    
    int getLeverage(String symbol);
    
    double getTotalFundingFees(String positionId);
    
    double getTakerFee();

    double getMarginBalance();

    double getOpenPrice(String positionId);

    double getWorstMarginBalance();
}
