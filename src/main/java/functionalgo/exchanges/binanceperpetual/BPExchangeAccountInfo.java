package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeAccountInfo;

public interface BPExchangeAccountInfo extends ExchangeAccountInfo {

    Double getLongQty(String symbol);

    Double getShortQty(String symbol);
    
    
    
}
