package functionalgo.exchanges.binanceperpetual;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocketFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import functionalgo.Utils;
import functionalgo.exceptions.ErrorParsingJsonException;

public class BPLiveExchange implements BPExchange {
    
    // TODO test if even if header limit not found still return response (should return)
    // TODO refactor AccountInfo to separate class on live and sim
    // TODO refactor api get calls to one method for similar enough code
    // TODO when instantiate set leverages to init and hedge mode and cross mode
    // TODO log every transaction, failures and issue
    // TODO make service methods to manually correct problems such as if the store was wiped
    // TODO when logging include json error message and http code
    // TODO include a logger as parameter to livexchange constructor
    // TODO rename ErrorParsingJsonException
    // TODO log on successes as well
    
    public static void main(String[] args) throws IOException {
        
        String privateKey = "***REMOVED***";
        String apiKey = "***REMOVED***";
        BPLiveStore store = new BPLiveFileAWSStore();
        BPExchange exchange = new BPLiveExchange(store, privateKey, apiKey);
        
        System.out.println("Sending test order...");
        
        exchange.getAccountInfo(System.currentTimeMillis());
        exchange.batchMarketOpen("ETHUSDT", true, 1.34562);
        exchange.batchMarketOpen("BTCUSDT", false, 0.12345);
        exchange.batchMarketClose("BCHUSDT", false, 0.1005);
        exchange.executeBatchedOrders();
        // TODO test "under load" see if prints as expected
        
        // TODO Auto-generated method stub
    }
    
    private static final byte[] IP_LIMIT_HEADER_WEIGHT_1M = "X-MBX-USED-WEIGHT-1M: ".getBytes(StandardCharsets.UTF_8);
    private static final int RES_BUFF_SIZE = 102400;
    
    private static final String HOST = "testnet.binancefuture.com";
    private static final String EXCHANGE_INFO_REQ = "GET /fapi/v1/exchangeInfo HTTP/1.1\r\nConnection: close\r\nHost: " + HOST
            + "\r\n\r\n";
    private static final String PREMIUM_INDEX_REQ = "GET /fapi/v1/premiumIndex HTTP/1.1\r\nConnection: close\r\nHost: " + HOST
            + "\r\n\r\n";
    private static final String AUTH = "X-MBX-APIKEY: ";
    private static final long TIMESTAMP_LAG = 5000;
    private static final long RECV_WINDOW = 15000;
    private static final String TRADING_STATUS = "TRADING";
    private static final double OPEN_LOSS = 1.03;
    
    private BPLiveStore store;
    private Mac signHMAC;
    private String apiKey;
    private SSLSocketFactory tlsSocketFactory;
    private int ipLimitWeight1M;
    private int httpStatusCode;
    
    private AccountInfo accountInfo;
    
    private List<BatchedOrder> batchedMarketOrders;
    
    private class BatchedOrder {
        
        String symbol;
        boolean isLong;
        double quantity;
        boolean isOpen;
        
        public BatchedOrder(String symbol, boolean isLong, double quantity, boolean isOpen) {
            
            this.symbol = symbol;
            this.isLong = isLong;
            this.quantity = quantity;
            this.isOpen = isOpen;
        }
    }
    
    class AccountInfo implements BPExchangeAccountInfo {
        
        class PositionData {
            
            double quantity;
            double avgOpenPrice;
            
            PositionData(double quantity, double avgOpenPrice) {
                
                this.quantity = quantity;
                this.avgOpenPrice = avgOpenPrice;
            }
        }
        
        class SymbolData {
            
            double fundingRate;
            double markPrice;
            long nextFundingTime;
            
            SymbolData(double fundingRate, double markPrice, long nextFundingTime) {
                
                this.fundingRate = fundingRate;
                this.markPrice = markPrice;
                this.nextFundingTime = nextFundingTime;
            }
        }
        
        private static final double TAKER_FEE = 0.0004;
        
        double totalInitialMargin = 0;
        double marginBalance = 0;
        double walletBalance = 0;
        long timestamp;
        boolean isHedgeMode;
        HashMap<String, Integer> leverages;
        HashMap<String, Boolean> isSymbolIsolated;
        HashMap<String, PositionData> longPositions;
        HashMap<String, PositionData> shortPositions;
        HashMap<String, PositionData> bothPositions;
        HashMap<String, SymbolData> symbolData;
        
        public AccountInfo() {
            
            // TODO no need for exchange store if accountinfo doesn't need to save permanent state
            leverages = new HashMap<>();
            isSymbolIsolated = new HashMap<>();
            longPositions = new HashMap<>();
            shortPositions = new HashMap<>();
            bothPositions = new HashMap<>();
            symbolData = new HashMap<>();
        }
        
        @Override
        public double getQuantity(String symbol, boolean isLong) {
            
            if (isLong) {
                return longPositions.get(symbol).quantity;
            } else {
                return shortPositions.get(symbol).quantity;
            }
        }
        
        @Override
        public long getTimestamp() {
            
            return timestamp;
        }
        
        @Override
        public double getWalletBalance() {
            
            return walletBalance;
        }
        
        @Override
        public int getLeverage(String symbol) {
            
            return leverages.get(symbol);
        }
        
        @Override
        public double getTakerFee() {
            
            return TAKER_FEE;
        }
        
        @Override
        public double getMarginBalance() {
            
            return marginBalance;
        }
        
        @Override
        public double getAverageOpenPrice(String symbol, boolean isLong) {
            
            if (isLong) {
                return longPositions.get(symbol).avgOpenPrice;
            } else {
                return shortPositions.get(symbol).avgOpenPrice;
            }
        }
        
        @Override
        public double getWorstMarginBalance() {
            
            return marginBalance;
        }
        
        @Override
        public long getNextFundingTime(String symbol) {
            
            return symbolData.get(symbol).nextFundingTime;
        }
        
        @Override
        public double getFundingRate(String symbol) {
            
            return symbolData.get(symbol).fundingRate;
        }
        
        @Override
        public double getMarkPrice(String symbol) {
            
            return symbolData.get(symbol).markPrice;
        }
    }
    
    public BPLiveExchange(BPLiveStore store, String privateKey, String apiKey) {
        
        try {
            this.store = store;
            this.apiKey = apiKey;
            
            signHMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec pKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            signHMAC.init(pKey);
            
            tlsSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            
            batchedMarketOrders = new ArrayList<>();
            
        } catch (Exception e) {
            // TODO throw custom exception
            // and log
        }
    }
    
    @Override
    public BPExchangeAccountInfo getAccountInfo(long timestamp) {
        
        // TODO store probably not needed
        accountInfo = store.getAccountInfo(apiKey);
        if (accountInfo == null) {
            // TODO log this
            accountInfo = new AccountInfo();
        }
        
        try {
            String jsonAccInfo = getAccountInformation();
            if (jsonAccInfo == null || jsonAccInfo.length() < 2) {
                throw new ErrorParsingJsonException("JSON account information response couldn't be parsed");
            }
            JsonElement elemAccInfo = JsonParser.parseString(jsonAccInfo);
            JsonObject objAccInfo = elemAccInfo.getAsJsonObject();
            accountInfo.totalInitialMargin = objAccInfo.get("totalInitialMargin").getAsDouble();
            accountInfo.marginBalance = objAccInfo.get("totalMarginBalance").getAsDouble();
            accountInfo.walletBalance = objAccInfo.get("totalWalletBalance").getAsDouble();
            JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
            for (JsonElement elem : arrPositions) {
                JsonObject elemObj = elem.getAsJsonObject();
                accountInfo.leverages.put(elemObj.get("symbol").getAsString(), elemObj.get("leverage").getAsInt());
                accountInfo.isSymbolIsolated.put(elemObj.get("symbol").getAsString(), elemObj.get("isolated").getAsBoolean());
                if (elemObj.get("positionSide").getAsString().equals("BOTH")) {
                    accountInfo.isHedgeMode = false;
                } else {
                    accountInfo.isHedgeMode = true;
                }
            }
            
            String jsonPosInfo = getPositionInformation();
            if (jsonPosInfo == null || jsonPosInfo.length() < 2) {
                throw new ErrorParsingJsonException("JSON position information response couldn't be parsed");
            }
            JsonElement elemPosInfo = JsonParser.parseString(jsonPosInfo);
            JsonArray arrPosInfo = elemPosInfo.getAsJsonArray();
            for (JsonElement elem : arrPosInfo) {
                JsonObject objElem = elem.getAsJsonObject();
                double quantity = objElem.get("positionAmt").getAsDouble();
                if (Math.abs(quantity) > 0) {
                    if (objElem.get("positionSide").getAsString().equals("LONG")) {
                        accountInfo.longPositions.put(objElem.get("symbol").getAsString(),
                                accountInfo.new PositionData(quantity, objElem.get("entryPrice").getAsDouble()));
                    } else if (objElem.get("positionSide").getAsString().equals("SHORT")) {
                        accountInfo.shortPositions.put(objElem.get("symbol").getAsString(),
                                accountInfo.new PositionData(Math.abs(quantity), objElem.get("entryPrice").getAsDouble()));
                    } else if (objElem.get("positionSide").getAsString().equals("BOTH")) {
                        accountInfo.bothPositions.put(objElem.get("symbol").getAsString(),
                                accountInfo.new PositionData(quantity, objElem.get("entryPrice").getAsDouble()));
                    } else {
                        throw new ErrorParsingJsonException("JSON position information symbol was not LONG, SHORT or BOTH");
                    }
                }
            }
            
            String jsonSymbolData = getPremiumIndex();
            if (jsonSymbolData == null || jsonSymbolData.length() < 2) {
                throw new ErrorParsingJsonException("JSON position information response couldn't be parsed");
            }
            JsonElement objSymbolData = JsonParser.parseString(jsonSymbolData);
            JsonArray arrSymbolData = objSymbolData.getAsJsonArray();
            for (JsonElement elem : arrSymbolData) {
                JsonObject objElem = elem.getAsJsonObject();
                accountInfo.symbolData.put(objElem.get("symbol").getAsString(),
                        accountInfo.new SymbolData(objElem.get("lastFundingRate").getAsDouble(),
                                objElem.get("markPrice").getAsDouble(), objElem.get("nextFundingTime").getAsLong()));
                accountInfo.timestamp = objElem.get("time").getAsLong();
            }
        } catch (Exception e) {
            // TODO log this, if it's not IOException, it's a json parsing problem
            e.printStackTrace();
            return null;
        }
        // TODO throttle ???
        
        return accountInfo;
    }
    
    @Override
    public boolean marketOpen(String symbol, boolean isLong, double symbolQty) {
        
        // TODO position management store in a db once request to exchange was successful
        
        try {
            sendMarketOpen(symbol, isLong, symbolQty);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return false;
    }
    
    public void sendMarketOpen(String symbol, boolean isLong, double symbolQty) throws IOException {
        // TODO limits throttling
        // TODO should return be a string json?
        
        long timestamp = getCurrentTimestampMillis();
        
        String orderString = "symbol=BTCUSDT&side=SELL&type=LIMIT&quantity=1.035&price=30000&timeInForce=GTC&recvWindow="
                + RECV_WINDOW + "&timestamp=" + timestamp;
        
        String signature = Utils.bytesToHex(signHMAC.doFinal(orderString.getBytes(StandardCharsets.UTF_8)));
        System.out.println(signature);
        
        String host = "testnet.binancefuture.com";
        String auth = "X-MBX-APIKEY: " + apiKey;
        String path = "/fapi/v1/order"
                + "?symbol=BTCUSDT&side=SELL&type=LIMIT&quantity=1.035&price=30000&timeInForce=GTC&recvWindow= " + RECV_WINDOW
                + "&timestamp=" + timestamp + "&signature=" + signature;
        
        try (Socket socket = tlsSocketFactory.createSocket(host, 443)) {
            
            String req = "POST " + path + " HTTP/1.1" + "\r\n" + "Connection: close" + "\r\n" + "Host: " + host + "\r\n" + auth
                    + "\r\n\r\n";
            
            OutputStream output = socket.getOutputStream();
            output.write(req.getBytes(StandardCharsets.UTF_8));
            output.flush();
            
            InputStream input = socket.getInputStream();
            
            byte[] buffer = new byte[1024];
            int length = 0;
            
            while ((length = input.read(buffer)) > 0) {
                System.out.write(buffer, 0, length);
            }
        }
        
    }
    
    @Override
    public boolean marketClose(String positionId, boolean isLong, double qtyToClose) {
        
        // TODO position management store in a db once request to exchange was successful
        
        return false;
    }
    
    @Override
    public void batchMarketOpen(String symbol, boolean isLong, double symbolQty) {
        
        batchedMarketOrders.add(new BatchedOrder(symbol, isLong, symbolQty, true));
        
    }
    
    @Override
    public void batchMarketClose(String symbol, boolean isLong, double qtyToClose) {
        
        batchedMarketOrders.add(new BatchedOrder(symbol, isLong, qtyToClose, false));
        
    }
    
    @Override
    public BPExchangeAccountInfo executeBatchedOrders() {
        
        // remove batch from list to execute whether or not it was successful
        ArrayList<BatchedOrder> tempBatch = new ArrayList<>(batchedMarketOrders);
        batchedMarketOrders.clear();
        
        // adjust the order quantity to the step size and check if there's enough margin to execute the orders
        JsonElement exchangeInfo;
        double sumInitialMargin = accountInfo.totalInitialMargin;
        try {
            String exchangeInfoJson = getExchangeInfo();
            if (exchangeInfoJson == null || exchangeInfoJson.length() < 2) {
                throw new ErrorParsingJsonException("JSON exchange information response couldn't be parsed");
            }
            exchangeInfo = JsonParser.parseString(exchangeInfoJson);
            JsonObject objExchangeInfo = exchangeInfo.getAsJsonObject();
            JsonArray arrExchangeInfoSymbols = objExchangeInfo.get("symbols").getAsJsonArray();
            boolean found = false;
            for (BatchedOrder order : tempBatch) {
                for (JsonElement elem : arrExchangeInfoSymbols) {
                    JsonObject objElem = elem.getAsJsonObject();
                    if (objElem.get("symbol").getAsString().equals(order.symbol)
                            && objElem.get("status").getAsString().equals(TRADING_STATUS)) {
                        JsonArray filter = objElem.get("filters").getAsJsonArray();
                        for (JsonElement filterElem : filter) {
                            JsonObject objFilter = filterElem.getAsJsonObject();
                            if (objFilter.get("filterType").getAsString().equals("LOT_SIZE")) {
                                double stepSize = objFilter.get("stepSize").getAsDouble();
                                if (order.quantity < stepSize) {
                                    throw new ErrorParsingJsonException("Market order quantity too low");
                                }
                                order.quantity = Math.floor(order.quantity / stepSize) * stepSize;
                                if (order.isOpen) {
                                    sumInitialMargin += ((order.quantity * accountInfo.getMarkPrice(order.symbol))
                                            / accountInfo.getLeverage(order.symbol)) * OPEN_LOSS;
                                } else {
                                    sumInitialMargin -= ((order.quantity * accountInfo.getMarkPrice(order.symbol))
                                            / accountInfo.getLeverage(order.symbol));
                                }
                                found = true;
                                break;
                            }
                        }
                        break;
                    }
                }
                if (!found) {
                    throw new ErrorParsingJsonException("Could not find expected members in exchangeInfo");
                } else {
                    found = false;
                }
            }
        } catch (Exception e) {
            // TODO log the event
            e.printStackTrace();
            return null;
        }
        if (sumInitialMargin >= accountInfo.marginBalance) {
            // TODO log insufficient margin to open
        }
        // TODO test sum initial margin if mark price and sum gets done properly
        
        // parse exchangeinfo in try or later and return null if exchangeinfo is null???
        
        // check if all of the orders will be executed, check necessary info with the exchangeInfo JSON element
        // if not, return null, empty batch lists regardless;
        // truncate as needed
        
        // TODO get mark price on getaccinfo, throttle getaccinfo
        
        for (BatchedOrder order : tempBatch) {
            if (order.isOpen) {
                if (sumInitialMargin < accountInfo.marginBalance) {
                    try {
                        // execute order
                    } catch (Exception e) {
                        // log this
                    }
                }
            } else {
                try {
                    // execute order
                } catch (Exception e) {
                    // log this
                }
            }
        }
        
        // TODO if successful update accountInfo, call /fapi/v2/balance to get updated balances
        // TODO include order error msgs in accountInfo
        // TODO if error in set, still return accountInfo, strat should not be concerned
        store.setAccountInfo(apiKey, accountInfo);
        
        return accountInfo;
    }
    
    private String getPremiumIndex() throws IOException {
        
        return apiGetRequestResponse(PREMIUM_INDEX_REQ);
    }
    
    private String getPositionInformation() throws IOException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String posInfoReq = "GET /fapi/v2/positionRisk?" + params + "&signature=" + signature
                + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
        
        return apiGetRequestResponse(posInfoReq);
    }
    
    private String getAccountInformation() throws IOException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String accInfoReq = "GET /fapi/v2/account?" + params + "&signature=" + signature
                + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
        
        return apiGetRequestResponse(accInfoReq);
    }
    
    private String getExchangeInfo() throws IOException {
        
        return apiGetRequestResponse(EXCHANGE_INFO_REQ);
    }
    
    private String apiGetRequestResponse(String request) throws IOException {
        
        try (Socket socket = tlsSocketFactory.createSocket(HOST, 443)) {
            
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes(StandardCharsets.UTF_8));
            output.flush();
            
            return getHTTPResponse(socket.getInputStream());
        }
    }
    
    /**
     * While parsing the htpp response the ipLimitWeight1M and httpStatusCode fields are set.
     * 
     * @param inputStream
     * @return the http response's message body
     * @throws IOException
     */
    private String getHTTPResponse(InputStream inputStream) throws IOException {
        
        BufferedInputStream input = new BufferedInputStream(inputStream);
        StringBuilder res = new StringBuilder(RES_BUFF_SIZE);
        // first parse status code and limit header
        int inChar;
        while ((inChar = input.read()) != 32) { // until a space if found
        }
        httpStatusCode = input.read() - 48; // convert to decimal
        while ((inChar = input.read()) >= 48) {
            httpStatusCode = (httpStatusCode * 10) + inChar - 48;
        }
        int prevChar = inChar;
        while ((inChar = input.read()) != -1) {
            if ((prevChar == IP_LIMIT_HEADER_WEIGHT_1M[0] || prevChar == IP_LIMIT_HEADER_WEIGHT_1M[0] + 32)
                    && (inChar == IP_LIMIT_HEADER_WEIGHT_1M[1] || inChar == IP_LIMIT_HEADER_WEIGHT_1M[1] + 32)) {
                for (int i = 2; i < IP_LIMIT_HEADER_WEIGHT_1M.length; i++) {
                    inChar = input.read();
                    if (inChar != IP_LIMIT_HEADER_WEIGHT_1M[i] && inChar != IP_LIMIT_HEADER_WEIGHT_1M[i] + 32) {
                        inChar = -2;
                        break;
                    }
                }
                if (inChar != -2) { // if true, limit header was found, parse its number
                    ipLimitWeight1M = input.read() - 48; // convert to decimal
                    while ((inChar = input.read()) >= 48) {
                        httpStatusCode = (httpStatusCode * 10) + inChar - 48;
                    }
                }
            } else if ((inChar == 13 && (inChar = input.read()) == 10)
                    && (prevChar == 10 || ((inChar = input.read()) == 13 && (inChar = input.read()) == 10))) {
                byte[] buffer = new byte[RES_BUFF_SIZE]; // reaching here means there was double cr lf, so parse response
                int length = 0;
                while ((length = input.read(buffer)) > 0) {
                    res.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
                }
            }
            prevChar = inChar;
        }
        return res.toString();
    }
    
    private long getCurrentTimestampMillis() {
        
        return System.currentTimeMillis() - TIMESTAMP_LAG;
    }
    
}
