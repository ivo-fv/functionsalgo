package functionsalgo.shared;

import functionsalgo.exceptions.ExchangeException;

public interface Strategy {

    Statistics execute(long timestamp) throws ExchangeException;

    boolean isLive();

}
