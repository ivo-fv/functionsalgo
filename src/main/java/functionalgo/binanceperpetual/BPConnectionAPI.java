package functionalgo.binanceperpetual;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BPConnectionAPI {
    
    private JsonElement accountInformation;
    private long accountInformationCacheTime = 0;
    
    public double getAccInfTotalInitialMargin(int millisecToCache) {
        
        return getCachedAccountInformation(millisecToCache).getAsJsonObject().get("totalInitialMargin").getAsDouble();
    }
    
    public double getAccInfMarginBalance(int millisecToCache) {
        
        return getCachedAccountInformation(millisecToCache).getAsJsonObject().get("totalMarginBalance").getAsDouble();
    }
    
    public double getAccInfWalletBalance(int millisecToCache) {
        
        return getCachedAccountInformation(millisecToCache).getAsJsonObject().get("totalWalletBalance").getAsDouble();
    }
    
    public Map<String, Integer> getAccInfLeverages(int millisecToCache) {
        
        JsonObject objAccInfo = getCachedAccountInformation(millisecToCache).getAsJsonObject();
        
        Map<String, Integer> leverages = new HashMap<>();
        
        JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();
            leverages.put(elemObj.get("symbol").getAsString(), elemObj.get("leverage").getAsInt());
        }
        
        return leverages;
    }
    
    public Map<String, Boolean> getAccInfIsolatedSymbols(int millisecToCache) {
        
        JsonObject objAccInfo = getCachedAccountInformation(millisecToCache).getAsJsonObject();
        
        Map<String, Boolean> isSymbolIsolated = new HashMap<>();
        
        JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();
            isSymbolIsolated.put(elemObj.get("symbol").getAsString(), elemObj.get("isolated").getAsBoolean());
        }
        
        return isSymbolIsolated;
    }
    
    public boolean isAccInfHedgeMode(int millisecToCache) {
        
        JsonObject objAccInfo = getCachedAccountInformation(millisecToCache).getAsJsonObject();
        
        JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();
            return elemObj.get("positionSide").getAsString().equals("BOTH");
        }
        
        return false;
    }
    
    private JsonElement getCachedAccountInformation(int millisecToCache) {
        
        if (accountInformationCacheTime > System.currentTimeMillis()) {
            return accountInformation;
        }
        
        accountInformationCacheTime = System.currentTimeMillis() + millisecToCache;
        accountInformation = getAccountInformation();
        
        return accountInformation;
    }
    
    private JsonElement getAccountInformation() {
        
        CloseableHttpClient httpclient = HttpClients.createMinimal();
        
        // TODO Auto-generated method stub
        return null;
    }
    
}
