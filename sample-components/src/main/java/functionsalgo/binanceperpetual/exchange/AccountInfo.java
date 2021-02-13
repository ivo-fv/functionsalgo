package functionsalgo.binanceperpetual.exchange;

import java.util.List;

public interface AccountInfo {

    long getTimestampMillis();

    double getWalletBalance();

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
