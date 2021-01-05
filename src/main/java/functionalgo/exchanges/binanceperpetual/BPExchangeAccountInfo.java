package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeAccountInfo;

public interface BPExchangeAccountInfo extends ExchangeAccountInfo {
    
    Double getLongQty(String symbol);
    
    Double getShortQty(String symbol);
    
    String[] getLongPositionSymbols();
    
    String[] getShortPositionSymbols();

    double getLongClosePriceWithSlippage(String symbol, double symbolQty);

    double getShortClosePriceWithSlippage(String symbol, double symbolQty);
    
}
