package functionalgo.binanceperpetual;

import java.util.Map;

public class BPAccountInfoWrapper {

    private double totalInitialMargin;
    private double marginBalance;
    private double walletBalance;
    private Map<String, Integer> leverages;
    private Map<String, Boolean> isolatedSymbols;
    private boolean isHedgeMode;
    private Map<String, Double> longPositions;
    private Map<String, Double> shortPositions;
    private Map<String, Double> bothPositions;

    BPAccountInfoWrapper(double totalInitialMargin, double marginBalance, double walletBalance,
            Map<String, Integer> leverages, Map<String, Boolean> isolatedSymbols, boolean isHedgeMode,
            Map<String, Double> longPositions, Map<String, Double> shortPositions, Map<String, Double> bothPositions) {

        this.totalInitialMargin = totalInitialMargin;
        this.marginBalance = marginBalance;
        this.walletBalance = walletBalance;
        this.leverages = leverages;
        this.isolatedSymbols = isolatedSymbols;
        this.isHedgeMode = isHedgeMode;
        this.longPositions = longPositions;
        this.shortPositions = shortPositions;
        this.bothPositions = bothPositions;
    }

    double getTotalInitialMargin() {
        return totalInitialMargin;
    }

    double getMarginBalance() {
        return marginBalance;
    }

    double getWalletBalance() {
        return walletBalance;
    }

    Map<String, Integer> getLeverages() {
        return leverages;
    }

    Map<String, Boolean> getIsolatedSymbols() {
        return isolatedSymbols;
    }

    boolean isHedgeMode() {
        return isHedgeMode;
    }

    Map<String, Double> getLongPositions() {
        return longPositions;
    }

    Map<String, Double> getShortPositions() {
        return shortPositions;
    }

    Map<String, Double> getBothPositions() {
        return bothPositions;
    }
}
