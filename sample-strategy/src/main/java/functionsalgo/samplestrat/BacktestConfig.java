package functionsalgo.samplestrat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Utils;

public class BacktestConfig {

    private static final Logger logger = LogManager.getLogger();

    private static final double DEFAULT_BP_INITIAL_BALANCE = 1000;
    private static final short DEFAULT_BP_DEFAULT_LEVERAGE = 2;
    private static final String DEFAULT_BP_KLINES = "binance_perp_default_klines_5m";
    private static final String DEFAULT_BP_FUNDING_RATES = "binance_perp_default_fund_rates";
    private static final String DEFAULT_BP_SLIPPAGE_MODEL = "binance_perp_default_slippage_model";

    private double bpInitialBalance = -1;
    private short bpDefaultLeverage = -1;
    private HistoricKlines bpKlines;
    private HistoricFundingRates bpFundingRates;
    private SlippageModel bpSlippageModel;

    public BacktestConfig() {
        // use the with methods to build the config
    }

    public BacktestConfig withBPInitialBalance(double bpInitialBalance) {
        this.bpInitialBalance = bpInitialBalance;
        return this;
    }

    public double getBPInitialBalance() {
        if (bpInitialBalance == -1) {
            logger.info(
                    "binance perpetual initial balance missing, you can configure it using the withBpInitialBalance method, using default initial balance: {}",
                    DEFAULT_BP_INITIAL_BALANCE);
            return DEFAULT_BP_INITIAL_BALANCE;
        }
        return bpInitialBalance;
    }

    public BacktestConfig withBpDefaultLeverage(short bpDefaultLeverage) {
        this.bpDefaultLeverage = bpDefaultLeverage;
        return this;
    }

    public short getBPDefaultLeverage() {
        if (bpDefaultLeverage == -1) {
            logger.info(
                    "binance perpetual default leverage missing, you can configure it using the withBpDefaultLeverage method, using default default leverage: {}",
                    DEFAULT_BP_DEFAULT_LEVERAGE);
            return DEFAULT_BP_DEFAULT_LEVERAGE;
        }
        return bpDefaultLeverage;
    }

    public BacktestConfig withBPKlines(HistoricKlines bpKlines) {
        this.bpKlines = bpKlines;
        return this;
    }

    public HistoricKlines getBPKlines() throws StandardJavaException {
        if (bpKlines == null) {
            logger.info(
                    "binance perpetual klines missing, you can configure it using the withBPKlines method, loading default HistoricKlines");
            return (HistoricKlines) Utils.loadObjectResource(DEFAULT_BP_KLINES);
        }
        return bpKlines;
    }

    public BacktestConfig withBPFundingRates(HistoricFundingRates bpFundingRates) {
        this.bpFundingRates = bpFundingRates;
        return this;
    }

    public HistoricFundingRates getBPFundingRates() throws StandardJavaException {
        if (bpFundingRates == null) {
            logger.info(
                    "binance perpetual funding rates missing, you can configure it using the withBPFundingRates method, loading default HistoricFundingRates");
            return (HistoricFundingRates) Utils.loadObjectResource(DEFAULT_BP_FUNDING_RATES);
        }
        return bpFundingRates;
    }

    public BacktestConfig withBPSlippageModel(SlippageModel bpSlippageModel) {
        this.bpSlippageModel = bpSlippageModel;
        return this;
    }

    public SlippageModel getBPSlippageModel() throws StandardJavaException {
        if (bpSlippageModel == null) {
            logger.info(
                    "binance perpetual slippage model missing, you can configure it using the withBPSlippageModel method, loading default SlippageModel");
            return (SlippageModel) Utils.loadObjectResource(DEFAULT_BP_SLIPPAGE_MODEL);
        }
        return bpSlippageModel;
    }
}
