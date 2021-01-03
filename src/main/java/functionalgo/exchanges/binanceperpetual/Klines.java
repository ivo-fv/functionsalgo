package functionalgo.exchanges.binanceperpetual;

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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class Klines implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private static class Candle implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private double open;
        private double high;
        private double low;
        private double close;
        private double vol;
        private double buyVol;
        private int trades; // number of trades
    }
    
    public static final String KLINES_FILE = "binance_perp_klines";
    public static final String JSON_DATA_FOLDER = "binance_json_data";
    
    short interval;
    
    private HashMap<String, HashMap<Long, Candle>> klines;
    
    public static void main(String[] args) throws JsonSyntaxException, JsonIOException, IOException {
        
        System.out.println("Creating the klines file...");
        
        List<File> klineFiles = new ArrayList<>();
        
        for (File file : (new File(JSON_DATA_FOLDER)).listFiles((dir, name) -> name.endsWith(".json"))) {
            
            String[] fileData = file.getName().split("_", 3);
            
            if (Character.isDigit(fileData[1].toCharArray()[0])) {
                klineFiles.add(file);
            }
        }
        
        if (!klineFiles.isEmpty()) {
            
            short interval = Short.parseShort(klineFiles.get(0).getName().split("_", 3)[1]);
            
            Klines klinesObj = new Klines(interval);
            
            Gson gson = new Gson();
            
            for (File file : klineFiles) {
                
                String[] fileData = file.getName().split("_", 2);
                String symbol = fileData[0];
                
                BigDecimal[][] parsedSymbolKlines = gson
                        .fromJson(new BufferedReader(new InputStreamReader(new FileInputStream(file))), BigDecimal[][].class);
                
                klinesObj.addSymbolData(symbol, parsedSymbolKlines);
            }
            
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(new File(KLINES_FILE))))) {
                
                out.writeObject(klinesObj);
                out.flush();
            }
        }
        
        System.out.println("Finished creating klines file.");
    }
    
    private void addSymbolData(String symbol, BigDecimal[][] parsedSymbolKlines) {
        
        HashMap<Long, Candle> symbolData = new HashMap<>();
        
        for (BigDecimal[] parsedCandle : parsedSymbolKlines) {
            
            long openTime = (parsedCandle[0].longValueExact() / 10000) * 10000;
            
            Candle candle = new Candle();
            candle.open = parsedCandle[1].doubleValue();
            candle.high = parsedCandle[2].doubleValue();
            candle.low = parsedCandle[3].doubleValue();
            candle.close = parsedCandle[4].doubleValue();
            candle.vol = parsedCandle[7].doubleValue();
            candle.buyVol = parsedCandle[10].doubleValue();
            candle.trades = parsedCandle[8].intValueExact();
            
            symbolData.put(openTime, candle);
        }
        
        klines.put(symbol, symbolData);
    }
    
    private Klines(short interval) {
        
        this.interval = interval;
        klines = new HashMap<>();
    }
    
    public static Klines loadKlines() {
        
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(KLINES_FILE))))) {
            return (Klines) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public double getOpen(String symbol, long timestamp) {
        
        return klines.get(symbol).get(timestamp).open;
    }
}
