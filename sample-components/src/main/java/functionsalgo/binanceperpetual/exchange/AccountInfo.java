package functionsalgo.binanceperpetual.exchange;

import java.util.List;
import java.util.Map;

import functionsalgo.binanceperpetual.PositionWrapper;

public interface AccountInfo {

    long getTimestampMillis();

    double getWalletBalance();

    Map<String, PositionWrapper> getLongPositions();

    Map<String, PositionWrapper> getShortPositions();

    int getLeverage(String symbol);

    double getTakerFee();

    double getMarginBalance();

    double getQuantity(String symbol, boolean isLong);

    double getAverageOpenPrice(String symbol, boolean isLong);

    boolean isSymbolIsolated(String symbol, boolean isLong);

    boolean isHedgeMode();

    List<OrderError> getOrderErrors();

    double getTotalInitialMargin();
}
