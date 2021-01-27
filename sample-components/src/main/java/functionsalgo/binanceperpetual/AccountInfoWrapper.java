package functionsalgo.binanceperpetual;

import java.util.Map;

public class AccountInfoWrapper {

    private double totalInitialMargin;
    private double marginBalance;
    private double walletBalance;
    private Map<String, Integer> leverages;
    private Map<String, PositionWrapper> longPositions;
    private Map<String, PositionWrapper> shortPositions;
    private Map<String, PositionWrapper> bothPositions;
    private boolean isHedgeMode;

    AccountInfoWrapper(double totalInitialMargin, double marginBalance, double walletBalance,
            Map<String, Integer> leverages, Map<String, PositionWrapper> longPositions,
            Map<String, PositionWrapper> shortPositions, Map<String, PositionWrapper> bothPositions,
            boolean isHedgeMode) {

        this.totalInitialMargin = totalInitialMargin;
        this.marginBalance = marginBalance;
        this.walletBalance = walletBalance;
        this.leverages = leverages;
        this.longPositions = longPositions;
        this.shortPositions = shortPositions;
        this.bothPositions = bothPositions;
        this.isHedgeMode = isHedgeMode;
    }

    public double getTotalInitialMargin() {
        return totalInitialMargin;
    }

    public double getMarginBalance() {
        return marginBalance;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public Map<String, Integer> getLeverages() {
        return leverages;
    }

    public Map<String, PositionWrapper> getLongPositions() {
        return longPositions;
    }

    public Map<String, PositionWrapper> getShortPositions() {
        return shortPositions;
    }

    public Map<String, PositionWrapper> getBothPositions() {
        return bothPositions;
    }

    public boolean isHedgeMode() {
        return isHedgeMode;
    }
}
