package functionsalgo.binanceperpetual;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import functionsalgo.datapoints.AdjustedTimestamp;
import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.StandardJavaException;

public class HistoricFundingRates implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger();

    static String DATA_DIR = "../.genresources/data";
    static String FUND_RATES_FILE = DATA_DIR + "/binance_perp_fund_rates";
    static String JSON_DATA_FOLDER = DATA_DIR + "/binance_perp_json_data/fund_rates";

    private long fundingIntervalMillis = Interval._8h.toMilliseconds();
    private HashMap<String, HashMap<Long, FundingRate>> rates;

    public static HistoricFundingRates pullFundingRates(List<String> symbols, long startTime, long endTime)
            throws StandardJavaException {

        return pullFundingRates(null, symbols, startTime, endTime);
    }

    public static HistoricFundingRates pullFundingRates(File fileToSaveFundingRatesTo, List<String> symbols,
            long startTime, long endTime) throws StandardJavaException {

        File fratesDirJSON = new File(JSON_DATA_FOLDER);
        if (!fratesDirJSON.exists()) {
            fratesDirJSON.mkdirs();
        }

        File fratesFile = fileToSaveFundingRatesTo == null ? new File(FUND_RATES_FILE) : fileToSaveFundingRatesTo;

        ArrayList<File> symbolsJSONFiles = new ArrayList<>();
        for (String symbol : symbols) {
            symbolsJSONFiles.add(new File(fratesDirJSON + "/" + symbol + ".json"));
        }

        downloadFundingRates(symbolsJSONFiles, symbols, startTime, endTime);

        try {
            generateFundingRatesFile(symbolsJSONFiles, symbols, fratesFile);
        } catch (IOException e) {
            throw new StandardJavaException(e);
        }

        return loadFundingRates(fratesFile);
    }

    public static void downloadFundingRates(ArrayList<File> symbolsJSONFiles, List<String> symbols, long startTime,
            long endTime) throws StandardJavaException {

        WrapperREST restAPI;
        try {
            restAPI = new WrapperREST("don't need a valid key", "for this use case");
            restAPI.saveFundingRates(symbolsJSONFiles, symbols, startTime, endTime);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            throw new StandardJavaException(e);
        }
    }

    private static void generateFundingRatesFile(List<File> symbolsJSONFiles, List<String> symbols, File fratesFile)
            throws IOException {

        logger.info("Generating the funding rates object file {}", fratesFile);

        if (!symbolsJSONFiles.isEmpty()) {

            HistoricFundingRates fundratesObj = new HistoricFundingRates();

            Gson gson = new Gson();

            for (int i = 0; i < symbolsJSONFiles.size(); i++) {

                FundingRate[] parsedSymbolFundRates = gson.fromJson(
                        new BufferedReader(new InputStreamReader(new FileInputStream(symbolsJSONFiles.get(i)))),
                        FundingRate[].class);

                fundratesObj.addSymbolFundRates(symbols.get(i), parsedSymbolFundRates);
            }

            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(fratesFile)))) {

                out.writeObject(fundratesObj);
                out.flush();
            }

            logger.info("Finished generating the funding rates object file {}", fratesFile);
        } else {
            throw new FileNotFoundException("No symbol json files to process");
        }
    }

    public static HistoricFundingRates loadFundingRates() throws StandardJavaException {
        return loadFundingRates(new File(FUND_RATES_FILE));
    }

    public static HistoricFundingRates loadFundingRates(File fratesFile) throws StandardJavaException {

        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fratesFile)))) {
            return (HistoricFundingRates) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new StandardJavaException(e);
        }
    }

    public static String getDirName() {

        return JSON_DATA_FOLDER;
    }

    private HistoricFundingRates() {

        rates = new HashMap<>();
    }

    public FundingRate getFundingRate(String symbol, AdjustedTimestamp timestamp) {
        return rates.get(symbol).get(timestamp.getTime());
    }

    public List<FundingRate> getFundingRates(String symbol, AdjustedTimestamp startTime, AdjustedTimestamp endTime) {

        List<FundingRate> returnFRates = new ArrayList<>();

        for (long time = startTime.getTime(); time <= endTime.getTime(); time += fundingIntervalMillis) {
            returnFRates.add(rates.get(symbol).get(time));
        }

        return returnFRates;
    }

    public Interval getFundingInterval() {
        return Interval._8h;
    }

    private void addSymbolFundRates(String symbol, FundingRate[] parsedSymbolFundRates) {

        HashMap<Long, FundingRate> rate = new HashMap<>();

        for (FundingRate funddata : parsedSymbolFundRates) {

            long time = (funddata.getFundingTime() / 10000) * 10000;

            rate.put(time, funddata);
        }

        rates.put(symbol, rate);
    }
}
