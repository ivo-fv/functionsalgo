package functionalgo.exchanges.binanceperpetual;

import functionalgo.ExchangeTradeExecutor;

public interface BPTradeExecutor extends ExchangeTradeExecutor {

    void closeLong(String symbol);

    void reduceLong(String symbol, Double double1);

    void closeShort(String symbol);

    void reduceShort(String symbol, Double double1);
    
}
