package functionsalgo.samplestrat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import functionsalgo.binanceperpetual.HistoricFundingRates;
import functionsalgo.binanceperpetual.HistoricKlines;
import functionsalgo.binanceperpetual.SlippageModel;
import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Timestamp;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Strategy;
import functionsalgo.shared.Utils;

public class BacktestConfiguration implements functionsalgo.shared.BacktestConfiguration {

    private double bpInitialBalance = -1;
    private short bpDefaultLeverage = -1;
    private List<String> bpSymbols;
    private HistoricKlines bpKlines;
    private HistoricFundingRates bpFundingRates;
    private SlippageModel bpSlippageModel;
    private String bpKlinesLocation;
    private String bpFundingRatesLocation;
    private String bpSlippageModelLocation;
    private Interval bpInterval;
    private Timestamp bpStartTime;
    private Timestamp bpEndTime;

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

    public BacktestConfiguration withBPSymbols(List<String> bpSymbols) {
        this.bpSymbols = bpSymbols;
        return this;
    }

    public List<String> getBPSymbols() throws StandardJavaException {
        if (bpSymbols == null) {
            throw new IllegalStateException(
                    "binance perpetual symbols missing, you can configure it using the withBPSymbols method, or load from a config file");
        }
        return bpSymbols;
    }

    public BacktestConfiguration withInterval(Interval bpInterval) {
        this.bpInterval = bpInterval;
        return this;
    }

    public Interval getBPInterval() throws StandardJavaException {
        if (bpInterval == null) {
            throw new IllegalStateException(
                    "binance perpetual interval missing, you can configure it using the withBPSymbols method, or load from a config file");
        }
        return bpInterval;
    }

    public BacktestConfiguration withBPStartTime(Timestamp bpStartTime) {
        this.bpStartTime = bpStartTime;
        return this;
    }

    public Timestamp getBPStartTime() {
        if (bpStartTime == null) {
            throw new IllegalStateException(
                    "binance perpetual start time missing, you can configure it using the withBpInitialBalance method, or load from a config file");
        }
        return bpStartTime;
    }

    public BacktestConfiguration withBPEndTime(Timestamp bpEndTime) {
        this.bpEndTime = bpEndTime;
        return this;
    }

    public Timestamp getBPEndTime() {
        if (bpEndTime == null) {
            throw new IllegalStateException(
                    "binance perpetual end time missing, you can configure it using the withBpInitialBalance method, or load from a config file");
        }
        return bpEndTime;
    }

    public BacktestConfiguration withBPKlines(HistoricKlines bpKlines) {
        this.bpKlines = bpKlines;
        return this;
    }

    public BacktestConfiguration withBPKlinesLocation(String bpKlinesLocation) {
        this.bpKlinesLocation = bpKlinesLocation;
        return this;
    }

    public HistoricKlines getBPKlines() throws StandardJavaException {
        if (bpKlines == null) {
            throw new IllegalStateException(
                    "binance perpetual klines missing, you can configure it using the withBPKlines method, or load from a config file");
        }
        return bpKlines;
    }

    public String getBPKlinesLocation() {
        return bpKlinesLocation;
    }

    public BacktestConfiguration withBPFundingRates(HistoricFundingRates bpFundingRates) {
        this.bpFundingRates = bpFundingRates;
        return this;
    }

    public BacktestConfiguration withBPFundingRatesLocation(String bpFundingRatesLocation) {
        this.bpFundingRatesLocation = bpFundingRatesLocation;
        return this;
    }

    public HistoricFundingRates getBPFundingRates() throws StandardJavaException {
        if (bpFundingRates == null) {
            throw new IllegalStateException(
                    "binance perpetual funding rates missing, you can configure it using the withBPFundingRates method, or load from a config file");
        }
        return bpFundingRates;
    }

    public String getBPFundingRatesLocation() {
        return bpFundingRatesLocation;
    }

    public BacktestConfiguration withBPSlippageModel(SlippageModel bpSlippageModel) {
        this.bpSlippageModel = bpSlippageModel;
        return this;
    }

    public BacktestConfiguration withBPSlippageModelLocation(String bpSlippageModelLocation) {
        this.bpSlippageModelLocation = bpSlippageModelLocation;
        return this;
    }

    public SlippageModel getBPSlippageModel() throws StandardJavaException {
        if (bpSlippageModel == null) {
            throw new IllegalStateException(
                    "binance perpetual slippage model missing, you can configure it using the withBPSlippageModel method, or load from a config file");
        }
        return bpSlippageModel;
    }

    public String getBPSlippageModelLocation() {
        return bpSlippageModelLocation;
    }

    @Override
    public void loadConfiguration(String configFile, boolean gen, boolean forceGen) throws StandardJavaException {

        URL configUrl;
        Properties config = new Properties();
        try {
            configUrl = Utils.getFileOrResource(configFile);
            config.load(configUrl.openStream());
        } catch (IOException e) {
            throw new StandardJavaException(e);
        }

        if (bpInitialBalance == -1) {
            bpInitialBalance = Double.valueOf(config.getProperty("BPInitialBalance"));
        }
        if (bpDefaultLeverage == -1) {
            bpDefaultLeverage = Short.valueOf(config.getProperty("BPDefaultLeverage"));
        }
        if (bpSymbols == null) {
            String symbolList = config.getProperty("BPSymbols");
            bpSymbols = Arrays.asList(symbolList.split(",", 0));
        }
        if (bpInterval == null) {
            bpInterval = Interval.parseString(config.getProperty("BPInterval"));
        }
        if (bpStartTime == null) {
            bpStartTime = new Timestamp(Long.valueOf(config.getProperty("BPStartTime")), bpInterval);
        }
        if (bpEndTime == null) {
            bpEndTime = new Timestamp(Long.valueOf(config.getProperty("BPEndTime")), bpInterval);
        }
        if (bpKlines == null) {
            String klinesPath = bpKlinesLocation == null ? config.getProperty("BPKlines") : bpKlinesLocation;
            if (forceGen) {
                bpKlines = HistoricKlines.pullKlines(new File(klinesPath), bpSymbols, bpInterval, bpStartTime,
                        bpEndTime);
            } else {
                try {
                    bpKlines = (HistoricKlines) Utils.loadObjectFileOrResource(klinesPath);
                } catch (StandardJavaException e) { // didn't exist so generate it
                    if (gen) {
                        bpKlines = HistoricKlines.pullKlines(new File(klinesPath), bpSymbols, bpInterval, bpStartTime,
                                bpEndTime);
                    } else {
                        throw new StandardJavaException(new IOException("no klines file to use"));
                    }
                }
            }
        }
        if (bpFundingRates == null) {
            String bpFundingRatesPath = bpFundingRatesLocation == null ? config.getProperty("BPFundingRates")
                    : bpFundingRatesLocation;
            if (forceGen) {
                bpFundingRates = HistoricFundingRates.pullFundingRates(new File(bpFundingRatesPath), bpSymbols,
                        bpStartTime, bpEndTime);
            } else {
                try {
                    bpFundingRates = (HistoricFundingRates) Utils.loadObjectFileOrResource(bpFundingRatesPath);
                } catch (StandardJavaException e) { // didn't exist so generate it
                    if (gen) {
                        bpFundingRates = HistoricFundingRates.pullFundingRates(new File(bpFundingRatesPath), bpSymbols,
                                bpStartTime, bpEndTime);
                    } else {
                        throw new StandardJavaException(new IOException("no funding rates file to use"));
                    }
                }
            }
        }
        if (bpSlippageModel == null) {
            String bpSlippageModelPath = bpSlippageModelLocation == null ? config.getProperty("BPSlippageModel")
                    : bpSlippageModelLocation;
            if (forceGen) {
                bpSlippageModel = SlippageModel.pullSlippageModel(bpSymbols, new File(bpSlippageModelPath));
            } else {
                try {
                    bpSlippageModel = (SlippageModel) Utils.loadObjectFileOrResource(bpSlippageModelPath);
                } catch (StandardJavaException e) { // didn't exist so generate it
                    if (gen) {
                        bpSlippageModel = SlippageModel.pullSlippageModel(bpSymbols, new File(bpSlippageModelPath));
                    } else {
                        throw new StandardJavaException(new IOException("no slippage model file to use"));
                    }
                }
            }
        }
    }

    @Override
    public Strategy getStrategy() throws StandardJavaException {
        return SampleStrategy.getBacktestStrategy(this);
    }

    @Override
    public Timestamp getBacktestStartTime() {
        // the example sample strat algo requires 12h of previous data so start later
        return bpStartTime.copy().add(Interval._12h.toMilliseconds());
    }

    @Override
    public Timestamp getBacktestEndTime() {
        return bpEndTime;
    }

    @Override
    public Interval getBacktestInterval() {
        return bpInterval;
    }
}
