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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import functionsalgo.exceptions.StandardJavaException;

public class SlippageModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Helper class to deserialize the order book json files.
     */
    @SuppressWarnings("unused")
    private static class OrderBookData implements Serializable {

        private static final long serialVersionUID = 1L;

        private long lastUpdateId;
        private long E;
        private long T;
        private double[][] bids;
        private double[][] asks;
    }

    private static final Logger logger = LogManager.getLogger();

    static String DATA_DIR = "../.genresources/data";
    static String MODEL_FILE = DATA_DIR + "/slippage_model";
    static String JSON_ORDER_BOOKS_FOLDER = DATA_DIR + "/binance_perp_json_order_books";

    static int MAX_RANGE_VALUE = 285000;
    static int SLIPPAGE_STEPS = 100;
    static int BOOK_DEPTH = 100;

    private Map<String, double[]> slippages;

    public static SlippageModel pullSlippageModel(List<String> symbols) throws StandardJavaException {

        return pullSlippageModel(symbols, new File(MODEL_FILE));
    }

    public static SlippageModel pullSlippageModel(List<String> symbols, File fileToSaveSlippageModelTo)
            throws StandardJavaException {

        File obDirJSON = new File(JSON_ORDER_BOOKS_FOLDER);
        if (!obDirJSON.exists()) {
            obDirJSON.mkdirs();
        }

        ArrayList<File> symbolsJSONFiles = new ArrayList<>();
        for (String symbol : symbols) {
            symbolsJSONFiles.add(new File(JSON_ORDER_BOOKS_FOLDER + "/" + symbol + ".json"));
        }

        downloadModelJSONS(symbolsJSONFiles, symbols, true);

        try {
            generateSlippageModel(symbolsJSONFiles, symbols, fileToSaveSlippageModelTo);
        } catch (IOException e) {
            throw new StandardJavaException(e);
        }

        return loadSlippageModel(fileToSaveSlippageModelTo);
    }

    public static void downloadModelJSONS(List<File> symbolsJSONFiles, List<String> symbols, boolean append)
            throws StandardJavaException {
        WrapperREST restAPI;
        try {
            restAPI = new WrapperREST("don't need a valid key", "for this use case");
            restAPI.saveOrderBooks(symbolsJSONFiles, symbols, BOOK_DEPTH, append);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            throw new StandardJavaException(e);
        }
    }

    public static void generateSlippageModel(ArrayList<File> symbolsJSONFiles, List<String> symbols,
            File fileToSaveSlippageModelTo) throws IOException {

        logger.info("Generating the SlippageModel object file {}", fileToSaveSlippageModelTo);

        if (symbolsJSONFiles.isEmpty()) {
            throw new FileNotFoundException("No symbol json files to process");
        }

        SlippageModel slippageModel = new SlippageModel();

        Gson gson = new Gson();

        for (int i = 0; i < symbolsJSONFiles.size(); i++) {

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(symbolsJSONFiles.get(i))))) {

                OrderBookData[] multipleOrderBookData = gson.fromJson(reader, OrderBookData[].class);

                slippageModel.calculateSymbol(symbols.get(i), multipleOrderBookData);
            }
        }

        File slippageModelFile = fileToSaveSlippageModelTo == null ? new File(MODEL_FILE) : fileToSaveSlippageModelTo;

        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(slippageModelFile)))) {

            out.writeObject(slippageModel);
            out.flush();
        }

        logger.info("Finished generating the SlippageModel object file {}", fileToSaveSlippageModelTo);
    }

    public static SlippageModel loadSlippageModel() {
        return loadSlippageModel(new File(MODEL_FILE));
    }

    public static SlippageModel loadSlippageModel(File slippageModelFile) {
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(slippageModelFile)))) {
            return (SlippageModel) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SlippageModel() {

        slippages = new HashMap<>();
    }

    /**
     * @param positionAmount
     * @param symbol
     * @return the slippage >= 1 the positionAmount (notional) would incur when
     *         market buying/selling with that positionAmount. A value of 1.012
     *         would mean a slippage of 1.2% , meaning there would be 1.2% less
     *         value gotten out of the order than expected
     */
    public double getSlippage(double positionAmount, String symbol) {

        if (positionAmount > MAX_RANGE_VALUE) {
            positionAmount = MAX_RANGE_VALUE;
        }

        return slippages.get(symbol)[(int) positionAmount / SLIPPAGE_STEPS];
    }

    /**
     * Calculates the slippage for the given symbol and stores it.
     * 
     * @param symbol
     * @param multipleOrderBookData
     */
    private void calculateSymbol(String symbol, OrderBookData[] multipleOrderBookData) {

        double[] slippageValues = new double[(MAX_RANGE_VALUE / SLIPPAGE_STEPS) + 1];

        for (int step = 0; step <= MAX_RANGE_VALUE; step += SLIPPAGE_STEPS) {

            double slippage = 0;

            for (OrderBookData book : multipleOrderBookData) {

                double marketAmount = step + SLIPPAGE_STEPS - 1;

                double bidAskMidPrice = (book.asks[0][0] + book.bids[0][0]) / 2;

                double quantityExpected = marketAmount / bidAskMidPrice;
                slippage += quantityExpected / calculateOneSideOfBookQuantity(book.asks, marketAmount);

                quantityExpected = marketAmount / bidAskMidPrice;
                slippage += calculateOneSideOfBookQuantity(book.bids, marketAmount) / quantityExpected;

            }

            slippageValues[step / SLIPPAGE_STEPS] = slippage / (multipleOrderBookData.length * 2);

        }

        slippages.put(symbol, slippageValues);
    }

    /**
     * how much of the symbol (quantity) in sideOfBook can be bought/sold with the
     * given marketAmount (notional)
     * 
     * @param sideOfBook
     * @param marketAmount
     * @return
     */
    private double calculateOneSideOfBookQuantity(double[][] sideOfBook, double marketAmount) {

        double quantity = 0;

        for (int entry = 0; entry < sideOfBook.length && marketAmount > 0; entry++) {

            double entryValue = sideOfBook[entry][0] * sideOfBook[entry][1];

            double amountLeftOver = marketAmount - entryValue;

            if (amountLeftOver > 0) {
                quantity += sideOfBook[entry][1];
                marketAmount = amountLeftOver;
            } else {
                quantity += marketAmount / sideOfBook[entry][0];
                marketAmount = 0;
            }
        }

        if (marketAmount > 0) {
            logger.info("Market amount exceeds book depth.");
        }
        return quantity;
    }
}
