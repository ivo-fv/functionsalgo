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
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import functionsalgo.datapoints.Interval;
import functionsalgo.datapoints.Kline;
import functionsalgo.exceptions.StandardJavaException;

public class HistoricKlines implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger();

    static String DATA_DIR = "../.genresources/data";
    static String KLINES_FILE_GENERIC_NAME = "binance_perp_klines_";
    static String JSON_DATA_DIR = DATA_DIR + "/binance_perp_json_data";

    private Interval interval;

    private HashMap<String, HashMap<Long, Kline>> klines;

    public static HistoricKlines pullKlines(List<String> symbols, Interval interval, long startTime, long endTime)
            throws StandardJavaException {

        return pullKlines(null, symbols, interval, startTime, endTime);
    }

    public static HistoricKlines pullKlines(File fileToSaveKlinesTo, List<String> symbols, Interval interval,
            long startTime, long endTime) throws StandardJavaException {

        String klinesDirJSONPath = getJSONDirName(interval);
        File klinesDirJSON = new File(klinesDirJSONPath);
        if (!klinesDirJSON.exists()) {
            klinesDirJSON.mkdirs();
        }

        File klinesFile = fileToSaveKlinesTo == null
                ? new File(DATA_DIR + "/" + KLINES_FILE_GENERIC_NAME + interval.toString())
                : fileToSaveKlinesTo;

        ArrayList<File> symbolsJSONFiles = new ArrayList<>();
        for (String symbol : symbols) {
            symbolsJSONFiles.add(new File(klinesDirJSONPath + "/" + symbol + ".json"));
        }

        downloadKlines(symbolsJSONFiles, symbols, interval, startTime, endTime);

        try {
            generateKlinesFile(symbolsJSONFiles, symbols, klinesFile, interval);
        } catch (IOException e) {
            throw new StandardJavaException(e);
        }

        return loadKlines(interval);
    }

    public static void downloadKlines(List<File> klinesSymbolsFilesJSON, List<String> symbols, Interval interval,
            long startTime, long endTime) throws StandardJavaException {
        WrapperREST restAPI;
        try {
            restAPI = new WrapperREST("don't need a valid key", "for this use case");
            restAPI.saveKlines(klinesSymbolsFilesJSON, symbols, interval, startTime, endTime);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            throw new StandardJavaException(e);
        }
    }

    public static void generateKlinesFile(List<File> klinesSymbolsFilesJSON, List<String> symbols, File klinesFile,
            Interval interval) throws IOException {

        logger.info("Generating the klines object file {}", klinesFile);

        if (!klinesSymbolsFilesJSON.isEmpty()) {

            HistoricKlines klinesObj = new HistoricKlines(interval);

            Gson gson = new Gson();

            for (int i = 0; i < klinesSymbolsFilesJSON.size(); i++) {

                BigDecimal[][] parsedSymbolKlines = gson.fromJson(
                        new BufferedReader(new InputStreamReader(new FileInputStream(klinesSymbolsFilesJSON.get(i)))),
                        BigDecimal[][].class);

                klinesObj.addSymbolData(symbols.get(i), parsedSymbolKlines);
            }

            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(klinesFile)))) {

                out.writeObject(klinesObj);
                out.flush();
            }

            logger.info("Finished generating the klines object file {}", klinesFile);
        } else {
            throw new FileNotFoundException("No symbol json files to process");
        }
    }

    public static HistoricKlines loadKlines(Interval interval) throws StandardJavaException {
        return loadKlines(new File(DATA_DIR + "/" + KLINES_FILE_GENERIC_NAME + interval.toString()));
    }

    public static HistoricKlines loadKlines(File klinesObject) throws StandardJavaException {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(klinesObject)))) {
            return (HistoricKlines) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new StandardJavaException(e);
        }
    }

    public static String getJSONDirName(Interval interval) {
        return JSON_DATA_DIR + "/klines_" + interval.toString();
    }

    private HistoricKlines(Interval interval) {
        this.interval = interval;
        klines = new HashMap<>();
    }

    public List<Kline> getKlines(String symbol, long startTime, long endTime) {

        long adjustedStartTime = (startTime / interval.toMilliseconds()) * interval.toMilliseconds();
        long adjustedEndTime = (endTime / interval.toMilliseconds()) * interval.toMilliseconds();
        List<Kline> returnKlines = new ArrayList<>();

        for (long time = adjustedStartTime; time <= adjustedEndTime; time += interval.toMilliseconds()) {
            returnKlines.add(klines.get(symbol).get(time));
        }

        return returnKlines;
    }

    public Interval getInterval() {

        return interval;
    }

    private void addSymbolData(String symbol, BigDecimal[][] parsedSymbolKlines) {

        HashMap<Long, Kline> symbolData = new HashMap<>();

        for (BigDecimal[] parsedCandle : parsedSymbolKlines) {

            long openTime = (parsedCandle[0].longValueExact() / 10000) * 10000;

            Kline candle = new Kline(parsedCandle[0].longValue(), parsedCandle[1].doubleValue(),
                    parsedCandle[2].doubleValue(), parsedCandle[3].doubleValue(), parsedCandle[4].doubleValue(),
                    parsedCandle[5].doubleValue(), parsedCandle[6].longValue(), parsedCandle[7].doubleValue(),
                    parsedCandle[8].intValue(), parsedCandle[9].doubleValue(), parsedCandle[10].doubleValue());

            symbolData.put(openTime, candle);
        }

        klines.put(symbol, symbolData);
    }
}
