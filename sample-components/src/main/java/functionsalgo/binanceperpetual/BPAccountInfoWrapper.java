package functionsalgo.binanceperpetual;

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

    public Map<String, Boolean> getIsolatedSymbols() {
        return isolatedSymbols;
    }

    public boolean isHedgeMode() {
        return isHedgeMode;
    }

    public Map<String, Double> getLongPositions() {
        return longPositions;
    }

    public Map<String, Double> getShortPositions() {
        return shortPositions;
    }

    public Map<String, Double> getBothPositions() {
        return bothPositions;
    }
}
