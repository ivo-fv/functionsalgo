package functionsalgo.binanceperpetual.dataprovider;

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

import functionsalgo.datapoints.FundingRate;
import functionsalgo.datapoints.Interval;

public class BPHistoricFundingRates implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private static final String FUND_RATES_FILE = "data/binance_perp_fund_rates";
    private static final String JSON_DATA_FOLDER = "data/binance_perp_json_data/fund_rates";
    
    private long fundingIntervalMillis = Interval._8h.toMilliseconds(); // 8 hours
    private HashMap<String, HashMap<Long, FundingRate>> rates;
    
    /**
     * Used for generating funding rates files from .json for use in backtesting
     */
    public static void main(String[] args) throws JsonSyntaxException, JsonIOException, IOException {
        
        System.out.println("Creating the funding rates file...");
        
        List<File> fundratesFiles = new ArrayList<>();
        
        for (File file : (new File(JSON_DATA_FOLDER)).listFiles((dir, name) -> name.endsWith(".json"))) {
            
            fundratesFiles.add(file);
        }
        
        if (!fundratesFiles.isEmpty()) {
            
            BPHistoricFundingRates fundratesObj = new BPHistoricFundingRates();
            
            Gson gson = new Gson();
            
            for (File file : fundratesFiles) {
                
                String[] fileData = file.getName().split("_", 2);
                String symbol = fileData[0];
                
                FundingRate[] parsedSymbolFundRates = gson
                        .fromJson(new BufferedReader(new InputStreamReader(new FileInputStream(file))), FundingRate[].class);
                
                fundratesObj.addSymbolFundRates(symbol, parsedSymbolFundRates);
            }
            
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(new File(FUND_RATES_FILE))))) {
                
                out.writeObject(fundratesObj);
                out.flush();
            }
        } else {
            System.out.println("Couldn't find the .json files.");
        }
        
        System.out.println("Finished creating funding rates file.");
    }
    
    private void addSymbolFundRates(String symbol, FundingRate[] parsedSymbolFundRates) {
        
        HashMap<Long, FundingRate> rate = new HashMap<>();
        
        for (FundingRate funddata : parsedSymbolFundRates) {
            
            long time = (funddata.getFundingTime() / 10000) * 10000;
            
            rate.put(time, funddata);
        }
        
        rates.put(symbol, rate);
    }
    
    private BPHistoricFundingRates() {
        
        rates = new HashMap<>();
    }
    
    public static BPHistoricFundingRates loadFundingRates() {
        
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(new File(FUND_RATES_FILE))))) {
            return (BPHistoricFundingRates) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public List<FundingRate> getFundingRates(String symbol, long startTime, long endTime) {
        
        long adjustedStartTime = (startTime / fundingIntervalMillis) * fundingIntervalMillis;
        long adjustedEndTime = (endTime / fundingIntervalMillis) * fundingIntervalMillis;
        List<FundingRate> returnFRates = new ArrayList<>();
        
        for (long time = adjustedStartTime; time <= adjustedEndTime; time += fundingIntervalMillis) {
            returnFRates.add(rates.get(symbol).get(time));
        }
        
        return returnFRates;
    }
    
    public long getFundingIntervalMillis() {
        
        return fundingIntervalMillis;
    }
    
    public static String getDirName() {
        
        return JSON_DATA_FOLDER;
    }
    
}
