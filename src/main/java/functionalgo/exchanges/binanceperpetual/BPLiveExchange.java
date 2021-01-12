package functionalgo.exchanges.binanceperpetual;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
import functionalgo.exceptions.ExchangeException;

public class BPLiveExchange implements BPExchange {
    
    // TODO test if even if header limit not found still return response (should return)
    // TODO refactor BPSimAccount to separate class on live and sim
    // TODO refactor api get calls to one method for similar enough code
    // TODO when instantiate set leverages to init and hedge mode and cross mode
    // TODO log every transaction, failures and issue
    // TODO make service methods to manually correct problems such as if the store was wiped
    // TODO when logging include json error message and http code
    // TODO include a logger as parameter to livexchange constructor
    // TODO rename ExchangeException
    // TODO log on successes as well
    
    public static void main(String[] args) throws IOException {
        
        String privateKey = "***REMOVED***";
        String apiKey = "***REMOVED***";
        
        BPExchange exchange = new BPLiveExchange(privateKey, apiKey);
        
        System.out.println("Sending test order...");
        
        exchange.getAccountInfo(System.currentTimeMillis());
        exchange.batchMarketOpen("id1", "ETHUSDT", true, 1.34562);
        exchange.batchMarketOpen("id2", "BTCUSDT", false, 0.12345);
        exchange.batchMarketClose("id3", "BCHUSDT", false, 0.1005);
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
    private static final int ERROR_CODE_UPPER_BOUND = 100;
    private static final String ENDPOINT_ACCOUNT_INFO = "/fapi/v2/account";
    private static final String ENDPOINT_POSITION_INFO = "/fapi/v2/positionRisk";
    private static final String ENDPOINT_ACCOUNT_BALANCE = "/fapi/v2/balance";
    private static final Object QUOTE_ASSET = "USDT";
    
    private Mac signHMAC;
    private String apiKey;
    private SSLSocketFactory tlsSocketFactory;
    private int ipLimitWeight1M;
    private int httpStatusCode;
    
    private BPLiveAccount accountInfo;
    
    private List<BatchedOrder> batchedMarketOrders;
    
    private class BatchedOrder {
        
        String orderId;
        String symbol;
        boolean isLong;
        double quantity;
        boolean isOpen;
        
        public BatchedOrder(String orderId, String symbol, boolean isLong, double quantity, boolean isOpen) {
            
            this.orderId = orderId;
            this.symbol = symbol;
            this.isLong = isLong;
            this.quantity = quantity;
            this.isOpen = isOpen;
        }
    }   
    
    public BPLiveExchange(String privateKey, String apiKey) {
        
        try {
            this.apiKey = apiKey;
            
            signHMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec pKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            signHMAC.init(pKey);
            
            tlsSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            
            batchedMarketOrders = new ArrayList<>();
            
        } catch (Exception e) {
            e.printStackTrace();
            // TODO throw custom exception
            // and log
        }
    }
    
    @Override
    public BPAccount getAccountInfo(long timestamp) {
        
        accountInfo = new BPLiveAccount();
        
        try {
            String jsonAccInfo = getAccountInformation();
            JsonElement elemAccInfo = parseStringAndCheckErrors(jsonAccInfo);
            JsonObject objAccInfo = elemAccInfo.getAsJsonObject();
            accountInfo.totalInitialMargin = objAccInfo.get("totalInitialMargin").getAsDouble();
            accountInfo.marginBalance = objAccInfo.get("totalMarginBalance").getAsDouble();
            accountInfo.walletBalance = objAccInfo.get("totalWalletBalance").getAsDouble();
            accountInfo.isBalancesDesynch = false;
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
            JsonElement elemPosInfo = parseStringAndCheckErrors(jsonPosInfo);
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
                        throw new ExchangeException(0, "",
                                "JSON position information symbol was not LONG, SHORT or BOTH: getAccountInfo");
                    }
                }
            }
            
            String jsonSymbolData = getPremiumIndex();
            JsonElement objSymbolData = parseStringAndCheckErrors(jsonSymbolData);
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
    
    private String marketOpen(String symbol, boolean isLong, double symbolQty) {
        
        // TODO position management store in a db once request to exchange was successful
        
        try {
            sendMarketOpen(symbol, isLong, symbolQty);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    private void sendMarketOpen(String symbol, boolean isLong, double symbolQty) throws IOException {
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
    
    private String marketClose(String positionId, boolean isLong, double qtyToClose) {
        
        return null;
    }
    
    @Override
    public void batchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty) {
        
        accountInfo.ordersWithErrors.remove(orderId);
        batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, symbolQty, true));
        
    }
    
    @Override
    public void batchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose) {
        
        accountInfo.ordersWithErrors.remove(orderId);
        batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, qtyToClose, false));
        
    }
    
    @Override
    public BPAccount executeBatchedOrders() {
        
        // remove batch from list to execute whether or not it was successful
        ArrayList<BatchedOrder> tempBatch = new ArrayList<>(batchedMarketOrders);
        batchedMarketOrders.clear();
        
        // adjust the order quantity to the step size and check if there's enough margin to execute the orders
        JsonElement exchangeInfo;
        double sumInitialMargin = accountInfo.totalInitialMargin;
        try {
            String exchangeInfoJson = getExchangeInfo();
            exchangeInfo = parseStringAndCheckErrors(exchangeInfoJson);
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
                                    throw new ExchangeException(0, "", "Market order quantity too low: executeBatchedOrders");
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
                    throw new ExchangeException(0, "",
                            "Could not find expected JSON members in exchangeInfo: executeBatchedOrders");
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
        
        for (BatchedOrder order : tempBatch) {
            if (order.isOpen) {
                if (sumInitialMargin < accountInfo.marginBalance) {
                    try {
                        // TODO marketOpen(order.symbol, order.isLong, order.quantity);
                    } catch (Exception e) {
                        // TODO log this
                        // TODO add msg error to return accinfo
                        if (e instanceof ExchangeException) {
                            accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                        } else {
                            accountInfo.ordersWithErrors.put(order.orderId,
                                    new ExchangeException(-1, "Execution error", e.toString()));
                        }
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    // TODO marketClose(order.symbol, order.isLong, order.quantity);
                } catch (Exception e) {
                    // TODO log this
                    // TODO add msg error to return accinfo
                    if (e instanceof ExchangeException) {
                        accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                    } else {
                        accountInfo.ordersWithErrors.put(order.orderId,
                                new ExchangeException(-1, "Execution error", e.toString()));
                    }
                    e.printStackTrace();
                }
            }
        }
        
        try {
            String balancesJson = getAccountBalance();
            JsonElement balancesElem = parseStringAndCheckErrors(balancesJson);
            JsonArray balancesArr = balancesElem.getAsJsonArray();
            for (JsonElement elem : balancesArr) {
                JsonObject objElem = elem.getAsJsonObject();
                if (objElem.get("asset").getAsString().equals(QUOTE_ASSET)) {
                    accountInfo.walletBalance = objElem.get("balance").getAsDouble();
                    accountInfo.marginBalance = accountInfo.walletBalance + objElem.get("crossUnPnl").getAsDouble();
                    accountInfo.totalInitialMargin = accountInfo.marginBalance - objElem.get("availableBalance").getAsDouble();
                    accountInfo.isBalancesDesynch = false;
                    break;
                }
            }
        } catch (Exception e) {
            // TODO log this
            accountInfo.isBalancesDesynch = true;
        }
        
        return accountInfo;
    }
    
    private String getAccountBalance() throws IOException {
        
        return apiGetSignedRequestResponseNoParams(ENDPOINT_ACCOUNT_BALANCE);
    }
    
    private String getPremiumIndex() throws IOException {
        
        return apiGetRequestResponse(PREMIUM_INDEX_REQ);
    }
    
    private String getPositionInformation() throws IOException {
        
        return apiGetSignedRequestResponseNoParams(ENDPOINT_POSITION_INFO);
    }
    
    private String getAccountInformation() throws IOException {
        
        return apiGetSignedRequestResponseNoParams(ENDPOINT_ACCOUNT_INFO);
    }
    
    private String getExchangeInfo() throws IOException {
        
        return apiGetRequestResponse(EXCHANGE_INFO_REQ);
    }
    
    private String apiGetSignedRequestResponseNoParams(String endpoint) throws IOException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String accInfoReq = "GET " + endpoint + "?" + params + "&signature=" + signature
                + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
        
        return apiGetRequestResponse(accInfoReq);
    }
    
    private String apiGetRequestResponse(String request) throws IOException {
        
        // TODO retry a few times
        try (Socket socket = tlsSocketFactory.createSocket(HOST, 443)) {
            
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes(StandardCharsets.UTF_8));
            output.flush();
            
            return getHTTPResponse(socket.getInputStream());
        }
    }
    
    private JsonElement parseStringAndCheckErrors(String jsonString) throws Exception {
        
        try {
            if (jsonString == null || jsonString.length() < 2) {
                throw new ExchangeException(0, "", "JSON account information response couldn't be parsed");
            }
            JsonElement elem = JsonParser.parseString(jsonString);
            if (elem.isJsonObject()) {
                JsonObject objElem = elem.getAsJsonObject();
                if (objElem.has("code") && objElem.has("msg")) {
                    int code = objElem.get("code").getAsInt();
                    if (code < ERROR_CODE_UPPER_BOUND) {
                        throw new ExchangeException(code, objElem.get("msg").getAsString(), "parseStringAndCheckErrors");
                    }
                }
            }
            return elem;
        } catch (Exception e) {
            throw e;
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
