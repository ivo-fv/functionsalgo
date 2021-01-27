package functionsalgo.shared;

import functionsalgo.exceptions.ExchangeException;

public interface Strategy {

    TradeStatistics execute(long timestamp);

    boolean isLive();

}
