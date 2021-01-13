package functionalgo.exchanges.binanceperpetual;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    
    // TODO better error exceptions
    // TODO log every transaction, failures and issue
    // TODO make service methods to manually correct problems such as if the store was wiped
    // TODO when logging include json error message and http code
    // TODO include a logger as parameter to livexchange constructor
    // TODO rename ExchangeException
    // TODO log on successes as well
    
    public static void main(String[] args) throws IOException, ExchangeException {
        
        String privateKey = "***REMOVED***";
        String apiKey = "***REMOVED***";
        
        BPExchange exchange = new BPLiveExchange(privateKey, apiKey);
        
        System.out.println("Sending test order...");
        
        exchange.getAccountInfo(System.currentTimeMillis());
        // exchange.batchMarketOpen("id0", "ETHUSDT", true, 1.34562);
        // exchange.batchMarketClose("id1", "ETHUSDT", true, 1.34562);
        // exchange.batchMarketOpen("id2", "BTCUSDT", false, 0.02345);
        // exchange.batchMarketClose("id3", "BTCUSDT", false, 0.02345);
        exchange.executeBatchedOrders();
        
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
    private static final String ENDPOINT_NEW_ORDER = "/fapi/v1/order";
    private static final String ENDPOINT_CHANGE_LEVERAGE = "/fapi/v1/leverage";
    private static final String ENDPOINT_CHANGE_MARGIN_TYPE = "/fapi/v1/marginType";
    private static final String ENDPOINT_CHANGE_POSITION_MODE = "/fapi/v1/positionSide/dual";
    private static final String QUOTE_ASSET = "USDT";
    
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
            populateAccountBalances();
            
            populateAccountPositions();
            
            populateAccountMarkPriceFunding();
            
        } catch (Exception e) {
            // TODO log this, if it's not IOException, it's a json parsing problem
            e.printStackTrace();
            return null;
        }
        // TODO throttle ???
        
        return accountInfo;
    }
    
    @Override
    public void setHedgeMode() throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "dualSidePosition=true&recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        
        String res;
        try {
            res = apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_POSITION_MODE, params);
        } catch (IOException e) {
            // TODO log this
            throw new ExchangeException(6, "Problem sending the request", e.toString());
        }
        
        parseStringAndCheckErrors(res);
        
        if (accountInfo != null) {
            accountInfo.isHedgeMode = true;
        }
        
    }
    
    @Override
    public void setLeverage(String symbol, int leverage) throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "symbol=" + symbol + "&leverage=" + leverage + "&recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        
        String res;
        try {
            res = apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_LEVERAGE, params);
        } catch (IOException e) {
            // TODO log this
            throw new ExchangeException(6, "Problem sending the request", e.toString());
        }
        
        parseStringAndCheckErrors(res);
        
        if (accountInfo != null) {
            accountInfo.leverages.put(symbol, leverage);
        }
    }
    
    @Override
    public void setCrossMargin(String symbol) throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "symbol=" + symbol + "&marginType=CROSSED&recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        
        String res;
        try {
            res = apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_MARGIN_TYPE, params);
        } catch (IOException e) {
            // TODO log this
            throw new ExchangeException(6, "Problem sending the request", e.toString());
        }
        
        parseStringAndCheckErrors(res);
        
        if (accountInfo != null) {
            accountInfo.isSymbolIsolated.put(symbol, false);
        }
    }
    
    @Override
    public void batchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty) throws ExchangeException {
        
        if (accountInfo != null) {
            accountInfo.ordersWithErrors.remove(orderId);
            batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, symbolQty, true));
        } else {
            // TODO log this
            throw new ExchangeException(-3, "accountInfo was null", "Must call getAccountInfo");
        }
    }
    
    @Override
    public void batchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose) throws ExchangeException {
        
        if (accountInfo != null) {
            accountInfo.ordersWithErrors.remove(orderId);
            batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, qtyToClose, false));
        } else {
            // TODO log this
            throw new ExchangeException(-3, "accountInfo was null", "Must call getAccountInfo");
        }
    }
    
    @Override
    public BPAccount executeBatchedOrders() throws ExchangeException {
        
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
                                    throw new ExchangeException(0, order.symbol,
                                            "Market order quantity too low for symbol: " + order.symbol);
                                }
                                order.quantity = BigDecimal.valueOf(Math.floor(order.quantity / stepSize))
                                        .multiply(BigDecimal.valueOf(stepSize)).doubleValue();
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
            throw new ExchangeException(-2, "Pre execution error", e.toString());
        }
        
        if (sumInitialMargin >= accountInfo.marginBalance) {
            // TODO log insufficient margin to open
            System.out.println("Not enough margin!");
        }
        
        for (BatchedOrder order : tempBatch) {
            if (order.isOpen) {
                if (sumInitialMargin < accountInfo.marginBalance) {
                    try {
                        if (accountInfo.isHedgeMode) {
                            String marketOpenRes = marketOpenHedgeMode(order.symbol, order.isLong, order.quantity);
                            JsonElement elemMkt = parseStringAndCheckErrors(marketOpenRes);
                            JsonObject objMkt = elemMkt.getAsJsonObject();
                            if (objMkt.has("symbol") && objMkt.get("symbol").getAsString().equals(order.symbol)) {
                                // TODO log success
                                accountInfo.ordersWithQuantities.put(order.orderId, order.quantity);
                            } else {
                                throw new ExchangeException(-10, "returned json doesn't have expected members",
                                        "returned json doesn't have expected members");
                            }
                        } else {
                            throw new ExchangeException(3, "Only hedge mode orders supported",
                                    "Only hedge mode orders supported: executeBatchedOrders");
                        }
                    } catch (Exception e) {
                        // TODO log this
                        if (e instanceof ExchangeException) {
                            accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                        } else {
                            accountInfo.ordersWithErrors.put(order.orderId,
                                    new ExchangeException(-1, "Execution error", e.toString()));
                        }
                        e.printStackTrace(); // TODO remove after logger done
                    }
                }
            } else {
                try {
                    if (accountInfo.isHedgeMode) {
                        String marketCloseRes = marketCloseHedgeMode(order.symbol, order.isLong, order.quantity);
                        JsonElement elemMkt = parseStringAndCheckErrors(marketCloseRes);
                        JsonObject objMkt = elemMkt.getAsJsonObject();
                        if (objMkt.has("symbol") && objMkt.get("symbol").getAsString().equals(order.symbol)) {
                            // TODO log success
                            accountInfo.ordersWithQuantities.put(order.orderId, order.quantity);
                        } else {
                            throw new ExchangeException(-10, "returned json doesn't have expected members",
                                    "returned json doesn't have expected members");
                        }
                    } else {
                        throw new ExchangeException(3, "Only hedge mode orders supported",
                                "Only hedge mode orders supported: executeBatchedOrders");
                    }
                } catch (Exception e) {
                    // TODO log this
                    if (e instanceof ExchangeException) {
                        accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                    } else {
                        accountInfo.ordersWithErrors.put(order.orderId,
                                new ExchangeException(-1, "Execution error", e.toString()));
                    }
                    e.printStackTrace(); // TODO remove after logger done
                }
            }
        }
        
        try {
            updateAccountBalances();
        } catch (Exception e) {
            // TODO log this
            accountInfo.isBalancesDesynch = true;
            e.printStackTrace(); // TODO remove after logger done
        }
        
        try {
            populateAccountPositions();
        } catch (Exception e) {
            // TODO log this
            accountInfo.isPositionsDesynch = true;
            e.printStackTrace(); // TODO remove after logger done
        }
        
        return accountInfo;
    }
    
    private void populateAccountBalances() throws IOException, ExchangeException {
        
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
    }
    
    private void populateAccountPositions() throws IOException, ExchangeException {
        
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
        accountInfo.isPositionsDesynch = false;
    }
    
    private void populateAccountMarkPriceFunding() throws IOException, ExchangeException {
        
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
    }
    
    private void updateAccountBalances() throws IOException, ExchangeException {
        
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
    }
    
    private String getAccountBalance() throws IOException {
        
        return apiGetSignedRequestResponse(ENDPOINT_ACCOUNT_BALANCE);
    }
    
    private String getPremiumIndex() throws IOException {
        
        return apiGetRequestResponse(PREMIUM_INDEX_REQ);
    }
    
    private String getPositionInformation() throws IOException {
        
        return apiGetSignedRequestResponse(ENDPOINT_POSITION_INFO);
    }
    
    private String getAccountInformation() throws IOException {
        
        return apiGetSignedRequestResponse(ENDPOINT_ACCOUNT_INFO);
    }
    
    private String getExchangeInfo() throws IOException {
        
        return apiGetRequestResponse(EXCHANGE_INFO_REQ);
    }
    
    private String apiGetSignedRequestResponse(String endpoint) throws IOException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String req = "GET " + endpoint + "?" + params + "&signature=" + signature + " HTTP/1.1\r\nConnection: close\r\nHost: "
                + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
        
        return apiGetRequestResponse(req);
    }
    
    private String marketOpenHedgeMode(String symbol, boolean isLong, double symbolQty) throws IOException {
        
        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=BUY&positionSide=LONG" : "&side=SELL&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow=" + RECV_WINDOW
                + "&timestamp=" + timestamp;
        
        return apiPostSignedRequestGetResponse(ENDPOINT_NEW_ORDER, params);
    }
    
    private String marketCloseHedgeMode(String symbol, boolean isLong, double symbolQty) throws IOException {
        
        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=SELL&positionSide=LONG" : "&side=BUY&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow=" + RECV_WINDOW
                + "&timestamp=" + timestamp;
        
        return apiPostSignedRequestGetResponse(ENDPOINT_NEW_ORDER, params);
    }
    
    private String apiPostSignedRequestGetResponse(String endpoint, String params) throws IOException {
        
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String req = "POST " + endpoint + "?" + params + "&signature=" + signature + " HTTP/1.1\r\nConnection: close\r\nHost: "
                + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
        
        return apiGetRequestResponse(req);
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
    
    private JsonElement parseStringAndCheckErrors(String jsonString) throws ExchangeException {
        
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
            if (e instanceof ExchangeException) {
                throw e;
            } else {
                throw new ExchangeException(5, "Problem parsing JSON", e.toString());
            }
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
