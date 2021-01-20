package functionalgo.binanceperpetual.exchange;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import functionalgo.binanceperpetual.dataprovider.BPHistoricDataGrabber;

public class BPSlippageModel implements Serializable {
    
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
    
    public static final String MODEL_FILE = "data/slippage_model";
    public static final String JSON_ORDER_BOOKS_FOLDER = "data/binance_perp_json_order_books";
    
    /**
     * false = will only generate order books, appending to existing ones
     * true = will only calculate from existing order books
     */
    private static final boolean CALCULATE_SLIPPAGE = false;
    
    private static final short DEFAULT_DEPTH = 100;
    private static final int MAX_RANGE_VALUE = 285000;
    private static final int SLIPPAGE_STEPS = 100;
    
    private Map<String, double[]> slippages;
    
    /**
     * Used for generating a slippage model intended for backtesting.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        
        if (CALCULATE_SLIPPAGE) {
            
            System.out.println("Creating Model...");
            
            BPSlippageModel slippageModel = new BPSlippageModel();
            
            Gson gson = new Gson();
            
            for (String symbol : BPHistoricDataGrabber.SYMBOLS) {
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(new File(JSON_ORDER_BOOKS_FOLDER + "/" + symbol + ".json"))))) {
                    
                    OrderBookData[] multipleOrderBookData = gson.fromJson(reader, OrderBookData[].class);
                    
                    slippageModel.calculateSymbol(symbol, multipleOrderBookData);
                }
            }
            
            File slippageModelFile = new File(MODEL_FILE);
            
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(slippageModelFile)))) {
                
                out.writeObject(slippageModel);
                out.flush();
            }
            
            System.out.println("Model created successfully, saved in the file: " + MODEL_FILE);
            
        } else {
            System.out.println("Generating the order book files...");
            
            BPHistoricDataGrabber orderBookGrabber = new BPHistoricDataGrabber();
            
            File orderBookDir = new File(JSON_ORDER_BOOKS_FOLDER);
            orderBookDir.mkdir();
            
            for (String symbol : BPHistoricDataGrabber.SYMBOLS) {
                
                File outFile = new File(JSON_ORDER_BOOKS_FOLDER + "/" + symbol + ".json");
                
                orderBookGrabber.saveSymbolOrderBook(symbol, DEFAULT_DEPTH, outFile);
            }
            
            System.out.println("Order book files generated successfully.");
        }
    }
    
    private BPSlippageModel() {
        
        slippages = new HashMap<>();
    }
    
    public static BPSlippageModel LoadSlippageModel(String slippageModelFile) {
        
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(new File(slippageModelFile))))) {
            return (BPSlippageModel) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
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
            System.out.println("INFO: Market amount exceeds book depth.");
        }
        
        return quantity;
    }
    
    public double getSlippage(double positionAmount, String symbol) {
        
        if (positionAmount > MAX_RANGE_VALUE) {
            positionAmount = MAX_RANGE_VALUE;
        }
        
        return slippages.get(symbol)[(int) positionAmount / SLIPPAGE_STEPS];
    }
}
