package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeAccountInfo;

public interface BPExchangeAccountInfo extends ExchangeAccountInfo {
    
    double getLongQty(String symbol);
    
    double getShortQty(String symbol);
    
    String[] getLongPositionSymbols();
    
    String[] getShortPositionSymbols();

    double getLongClosePriceWithSlippage(String symbol, double notionalValue);

    double getShortClosePriceWithSlippage(String symbol, double notionalValue);
    
}
