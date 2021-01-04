package functionalgo.dataproviders.binanceperpetual;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class BPHistoricFundingRates implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static final String FUND_RATES_FILE = "data/binance_perp_fund_rates";
    public static final String JSON_DATA_FOLDER = "data/binance_perp_json_data";
    
    private HashMap<String, HashMap<Long, Double>> rates;
    
    /**
     * Helper class to deserialize the funding rate json files to be used in the buildFundingRateTable method.
     */
    private static class FundingRateData implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @SuppressWarnings("unused")
        private String symbol;
        private long fundingTime;
        private double fundingRate;
    }
    
    public static void main(String[] args) throws JsonSyntaxException, JsonIOException, IOException {
        
        System.out.println("Creating the funding rates file...");
        
        List<File> fundratesFiles = new ArrayList<>();
        
        for (File file : (new File(JSON_DATA_FOLDER)).listFiles((dir, name) -> name.endsWith(".json"))) {
            
            String[] fileData = file.getName().split("_", 3);
            
            if (fileData[1].equals("FundingRate")) {
                fundratesFiles.add(file);
            }
        }
        
        if (!fundratesFiles.isEmpty()) {
            
            BPHistoricFundingRates fundratesObj = new BPHistoricFundingRates();
            
            Gson gson = new Gson();
            
            for (File file : fundratesFiles) {
                
                String[] fileData = file.getName().split("_", 2);
                String symbol = fileData[0];
                
                FundingRateData[] parsedSymbolFundRates = gson
                        .fromJson(new BufferedReader(new InputStreamReader(new FileInputStream(file))), FundingRateData[].class);
                
                fundratesObj.addSymbolFundRates(symbol, parsedSymbolFundRates);
            }
            
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(new File(FUND_RATES_FILE))))) {
                
                out.writeObject(fundratesObj);
                out.flush();
            }
        }
        
        System.out.println("Finished creating funding rates file.");
    }
    
    private void addSymbolFundRates(String symbol, FundingRateData[] parsedSymbolFundRates) {
        
        HashMap<Long, Double> rate = new HashMap<>();
        
        for (FundingRateData funddata : parsedSymbolFundRates) {
            
            long time = (funddata.fundingTime / 10000) * 10000;
            
            rate.put(time, funddata.fundingRate);
        }
        
        rates.put(symbol, rate);
    }
    
    private BPHistoricFundingRates() {
        
        rates = new HashMap<>();
    }
    
    public static BPHistoricFundingRates loadFundingRates(String file) {
        
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(file))))) {
            return (BPHistoricFundingRates) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public double getRate(String symbol, long timestamp) {
        
        return rates.get(symbol).get(timestamp);
    }
    
}
