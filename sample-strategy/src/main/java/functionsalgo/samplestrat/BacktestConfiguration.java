package functionsalgo.samplestrat;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Strategy;

public class BacktestConfiguration implements functionsalgo.shared.BacktestConfiguration {
    // TODO default config properties as a resource
    // TODO load a config properties (constructor)

    private double bpInitialBalance = -1;
    private short bpDefaultLeverage = -1;
    private HistoricKlines bpKlines;
    private HistoricFundingRates bpFundingRates;
    private SlippageModel bpSlippageModel;

    public BacktestConfiguration() {
        // use the with methods to build the config
    }

    public BacktestConfiguration withBPInitialBalance(double bpInitialBalance) {
        this.bpInitialBalance = bpInitialBalance;
        return this;
    }

    public double getBPInitialBalance() {
        if (bpInitialBalance == -1) {
            throw new IllegalStateException(
                    "binance perpetual initial balance missing, you can configure it using the withBpInitialBalance method, or load from a config file");
        }
        return bpInitialBalance;
    }

    public BacktestConfiguration withBpDefaultLeverage(short bpDefaultLeverage) {
        this.bpDefaultLeverage = bpDefaultLeverage;
        return this;
    }

    public short getBPDefaultLeverage() {
        if (bpDefaultLeverage == -1) {
            throw new IllegalStateException(
                    "binance perpetual default leverage missing, you can configure it using the withBpDefaultLeverage method, or load from a config file");
        }
        return bpDefaultLeverage;
    }

    public BacktestConfiguration withBPKlines(HistoricKlines bpKlines) {
        this.bpKlines = bpKlines;
        return this;
    }

    public HistoricKlines getBPKlines() throws StandardJavaException {
        if (bpKlines == null) {
            throw new IllegalStateException(
                    "binance perpetual klines missing, you can configure it using the withBPKlines method, or load from a config file");
        }
        return bpKlines;
    }

    public BacktestConfiguration withBPFundingRates(HistoricFundingRates bpFundingRates) {
        this.bpFundingRates = bpFundingRates;
        return this;
    }

    public HistoricFundingRates getBPFundingRates() throws StandardJavaException {
        if (bpFundingRates == null) {
            throw new IllegalStateException(
                    "binance perpetual funding rates missing, you can configure it using the withBPFundingRates method, or load from a config file");
        }
        return bpFundingRates;
    }

    public BacktestConfiguration withBPSlippageModel(SlippageModel bpSlippageModel) {
        this.bpSlippageModel = bpSlippageModel;
        return this;
    }

    public SlippageModel getBPSlippageModel() throws StandardJavaException {
        if (bpSlippageModel == null) {
            throw new IllegalStateException(
                    "binance perpetual slippage model missing, you can configure it using the withBPSlippageModel method, or load from a config file");
        }
        return bpSlippageModel;
    }

    @Override
    public void generateConfiguration() {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadConfiguration() {
        // TODO Auto-generated method stub

    }

    @Override
    public Strategy getStrategy() {
        // TODO Auto-generated method stub
        return null;
    }

}
