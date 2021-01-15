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

import awsadapters.AWSLogger;
import functionalgo.Logger;
import functionalgo.Utils;
import functionalgo.exceptions.ExchangeException;

public class BPLiveExchange implements BPExchange {
    
    public static void main(String[] args) throws ExchangeException {
        
        String privateKey = "***REMOVED***";
        String apiKey = "***REMOVED***";
        
        Logger logger = new AWSLogger();
        
        BPExchange exchange = new BPLiveExchange(logger, privateKey, apiKey);
        
        System.out.println("Sending test order...");
        
        BPAccount accInfo = exchange.getAccountInfo(System.currentTimeMillis());
        
        if (!accInfo.isHedgeMode()) {
            exchange.setHedgeMode();
        }
        if (accInfo.isSymbolIsolated("ETHUSDT")) {
            exchange.setCrossMargin("ETHUSDT");
        }
        if (accInfo.getLeverage("ETHUSDT") != 20) {
            exchange.setLeverage("ETHUSDT", 20);
        }
        exchange.batchMarketOpen("id0", "ETHUSDT", true, 1.34562);
        exchange.batchMarketClose("id1", "ETHUSDT", true, 1.34562);
        exchange.batchMarketOpen("id2", "BTCUSDT", false, 0.02345);
        exchange.batchMarketClose("id3", "BTCUSDT", false, 0.02345);
        exchange.executeBatchedOrders();
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
    private static final int NO_ERROR_CODE_LOWER_BOUND = 0;
    private static final int NO_ERROR_CODE_UPPER_BOUND = 299;
    private static final String ENDPOINT_ACCOUNT_INFO = "/fapi/v2/account";
    private static final String ENDPOINT_POSITION_INFO = "/fapi/v2/positionRisk";
    private static final String ENDPOINT_ACCOUNT_BALANCE = "/fapi/v2/balance";
    private static final String ENDPOINT_NEW_ORDER = "/fapi/v1/order";
    private static final String ENDPOINT_CHANGE_LEVERAGE = "/fapi/v1/leverage";
    private static final String ENDPOINT_CHANGE_MARGIN_TYPE = "/fapi/v1/marginType";
    private static final String ENDPOINT_CHANGE_POSITION_MODE = "/fapi/v1/positionSide/dual";
    private static final String QUOTE_ASSET = "USDT";
    private static final int NUM_RETRIES = 4;
    private static final int RETRY_TIME_MILLIS = 200;
    private static final int LIMIT_ROOM = 20;
    private static final long LIMIT_HIT_SLEEP_TIME = 60000;
    
    private Mac signHMAC;
    private String apiKey;
    private SSLSocketFactory tlsSocketFactory;
    private int maxIpLimitWeight1M = Integer.MAX_VALUE;
    private int ipLimitWeight1M;
    private int httpStatusCode;
    private Logger logger;
    
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
    
    public BPLiveExchange(Logger logger, String privateKey, String apiKey) throws ExchangeException {
        
        try {
            this.apiKey = apiKey;
            this.logger = logger;
            signHMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec pKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            signHMAC.init(pKey);
            
            tlsSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            
            batchedMarketOrders = new ArrayList<>();
            
        } catch (Exception e) {
            logger.log(5, -1, "BPLiveExchange", e.toString());
            throw new ExchangeException(-1, "BPLiveExchange", e.toString());
        }
    }
    
    @Override
    public BPAccount getAccountInfo(long timestamp) throws ExchangeException {
        
        accountInfo = new BPLiveAccount();
        
        try {
            JsonElement exchangeInfo = getExchangeInfo();
            JsonObject objExchangeInfo = exchangeInfo.getAsJsonObject();
            JsonArray arrExchangeInfoLimits = objExchangeInfo.get("rateLimits").getAsJsonArray();
            for (JsonElement elem : arrExchangeInfoLimits) {
                JsonObject elemObj = elem.getAsJsonObject();
                if (elemObj.get("rateLimitType").getAsString().equals("REQUEST_WEIGHT")) {
                    maxIpLimitWeight1M = elemObj.get("limit").getAsInt();
                    break;
                }
            }
            
            populateAccountBalances();
            
            populateAccountPositions();
            
            populateAccountMarkPriceFunding();
            
        } catch (Exception e) {
            logger.log(5, -2, "getAccountInfo", e.toString());
            if (e instanceof ExchangeException) {
                throw (ExchangeException) e;
            } else {
                throw new ExchangeException(-2, "getAccountInfo", e.toString());
            }
        }
        
        return accountInfo;
    }
    
    @Override
    public void setHedgeMode() throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "dualSidePosition=true&recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        
        apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_POSITION_MODE, params);
        
        if (accountInfo != null) {
            accountInfo.isHedgeMode = true;
            logger.log(0, 0, "setHedgeMode", "OK");
        }
    }
    
    @Override
    public void setLeverage(String symbol, int leverage) throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "symbol=" + symbol + "&leverage=" + leverage + "&recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        
        apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_LEVERAGE, params);
        
        if (accountInfo != null) {
            accountInfo.leverages.put(symbol, leverage);
            logger.log(0, 0, "setLeverage: " + symbol + ";" + leverage, "OK");
        }
    }
    
    @Override
    public void setCrossMargin(String symbol) throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "symbol=" + symbol + "&marginType=CROSSED&recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        
        apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_MARGIN_TYPE, params);
        
        if (accountInfo != null) {
            accountInfo.isSymbolIsolated.put(symbol, false);
            logger.log(0, 0, "setCrossMargin: " + symbol, "OK");
        }
    }
    
    @Override
    public void batchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty) throws ExchangeException {
        
        if (accountInfo != null) {
            accountInfo.ordersWithErrors.remove(orderId);
            batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, symbolQty, true));
        } else {
            logger.log(2, -3, "batchMarketOpen", "Must have called getAccountInfo");
            throw new ExchangeException(-3, "batchMarketOpen", "Must have called getAccountInfo");
        }
    }
    
    @Override
    public void batchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose) throws ExchangeException {
        
        if (accountInfo != null) {
            accountInfo.ordersWithErrors.remove(orderId);
            batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, qtyToClose, false));
        } else {
            logger.log(2, -3, "batchMarketClose", "Must have called getAccountInfo");
            throw new ExchangeException(-3, "batchMarketClose", "Must have called getAccountInfo");
        }
    }
    
    @Override
    public BPAccount executeBatchedOrders() throws ExchangeException {
        
        // remove batch from list to execute whether or not it was successful
        ArrayList<BatchedOrder> tempBatch = new ArrayList<>(batchedMarketOrders);
        batchedMarketOrders.clear();
        
        // adjust the order quantity to the step size and check if there's enough margin to execute the orders
        double sumInitialMargin = accountInfo.totalInitialMargin;
        try {
            JsonElement exchangeInfo = getExchangeInfo();
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
                                    throw new ExchangeException(-4, order.symbol,
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
                    throw new ExchangeException(-5, "executeBatchedOrders",
                            "Could not find expected JSON members in exchangeInfo: executeBatchedOrders");
                } else {
                    found = false;
                }
            }
        } catch (Exception e) {
            if (e instanceof ExchangeException) {
                logger.log(4, ((ExchangeException) e).getCode(), ((ExchangeException) e).getResponseMsg(),
                        ((ExchangeException) e).getExceptionMsg());
                throw e;
            } else {
                logger.log(4, -6, "executeBatchedOrders", e.toString());
                throw new ExchangeException(-6, "executeBatchedOrders", e.toString());
            }
        }
        
        if (sumInitialMargin >= accountInfo.marginBalance) {
            logger.log(2, -15, "executeBatchedOrders", "Not enough margin to open");
        }
        
        for (BatchedOrder order : tempBatch) {
            if (order.isOpen) {
                if (sumInitialMargin < accountInfo.marginBalance) {
                    try {
                        if (accountInfo.isHedgeMode) {
                            JsonElement elemMkt = marketOpenHedgeMode(order.symbol, order.isLong, order.quantity);
                            JsonObject objMkt = elemMkt.getAsJsonObject();
                            if (objMkt.has("symbol") && objMkt.get("symbol").getAsString().equals(order.symbol)) {
                                accountInfo.ordersWithQuantities.put(order.orderId, order.quantity);
                                logger.log(0, 0, "executeBatchedOrders", "Executed: OPEN " + order.symbol
                                        + (order.isLong ? " LONG " : " SHORT ") + order.quantity);
                            } else {
                                throw new ExchangeException(-7, "executeBatchedOrders",
                                        "returned json doesn't have expected members");
                            }
                        } else {
                            throw new ExchangeException(-8, "executeBatchedOrders",
                                    "Only hedge mode orders supported: executeBatchedOrders");
                        }
                    } catch (Exception e) {
                        if (e instanceof ExchangeException) {
                            accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                            logger.log(3, ((ExchangeException) e).getCode(), ((ExchangeException) e).getResponseMsg(),
                                    ((ExchangeException) e).getExceptionMsg());
                        } else {
                            accountInfo.ordersWithErrors.put(order.orderId,
                                    new ExchangeException(-9, "executeBatchedOrders", e.toString()));
                            logger.log(3, -9, "executeBatchedOrders", e.toString());
                            
                        }
                    }
                }
            } else {
                try {
                    if (accountInfo.isHedgeMode) {
                        JsonElement elemMkt = marketCloseHedgeMode(order.symbol, order.isLong, order.quantity);
                        JsonObject objMkt = elemMkt.getAsJsonObject();
                        if (objMkt.has("symbol") && objMkt.get("symbol").getAsString().equals(order.symbol)) {
                            accountInfo.ordersWithQuantities.put(order.orderId, order.quantity);
                            logger.log(0, 0, "executeBatchedOrders",
                                    "Executed: CLOSE" + order.symbol + (order.isLong ? " LONG " : " SHORT ") + order.quantity);
                        } else {
                            throw new ExchangeException(-7, "executeBatchedOrders",
                                    "returned json doesn't have expected members");
                        }
                    } else {
                        throw new ExchangeException(-8, "executeBatchedOrders",
                                "Only hedge mode orders supported: executeBatchedOrders");
                    }
                } catch (Exception e) {
                    if (e instanceof ExchangeException) {
                        accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                        logger.log(3, ((ExchangeException) e).getCode(), ((ExchangeException) e).getResponseMsg(),
                                ((ExchangeException) e).getExceptionMsg());
                    } else {
                        accountInfo.ordersWithErrors.put(order.orderId,
                                new ExchangeException(-9, "executeBatchedOrders", e.toString()));
                        logger.log(3, -9, "executeBatchedOrders", e.toString());
                    }
                }
            }
        }
        
        try {
            updateAccountBalances();
        } catch (ExchangeException e) {
            accountInfo.isBalancesDesynch = true;
            logger.log(4, e.getCode(), e.getResponseMsg(), e.getExceptionMsg());
        }
        
        try {
            populateAccountPositions();
        } catch (ExchangeException e) {
            accountInfo.isPositionsDesynch = true;
            logger.log(4, e.getCode(), e.getResponseMsg(), e.getExceptionMsg());
        }
        
        return accountInfo;
    }
    
    private void populateAccountBalances() throws ExchangeException {
        
        JsonElement elemAccInfo = getAccountInformation();
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
    
    private void populateAccountPositions() throws ExchangeException {
        
        JsonElement elemPosInfo = getPositionInformation();
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
                    throw new ExchangeException(-10, "populateAccountPositions",
                            "JSON position information symbol was not LONG, SHORT or BOTH: populateAccountPositions");
                }
            }
        }
        accountInfo.isPositionsDesynch = false;
    }
    
    private void populateAccountMarkPriceFunding() throws ExchangeException {
        
        JsonElement objSymbolData = getPremiumIndex();
        JsonArray arrSymbolData = objSymbolData.getAsJsonArray();
        for (JsonElement elem : arrSymbolData) {
            JsonObject objElem = elem.getAsJsonObject();
            accountInfo.symbolData.put(objElem.get("symbol").getAsString(),
                    accountInfo.new SymbolData(objElem.get("lastFundingRate").getAsDouble(),
                            objElem.get("markPrice").getAsDouble(), objElem.get("nextFundingTime").getAsLong()));
            accountInfo.timestamp = objElem.get("time").getAsLong();
        }
    }
    
    private void updateAccountBalances() throws ExchangeException {
        
        JsonElement balancesElem = getAccountBalance();
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
    
    private JsonElement getAccountBalance() throws ExchangeException {
        
        return apiGetSignedRequestResponse(ENDPOINT_ACCOUNT_BALANCE);
    }
    
    private JsonElement getPremiumIndex() throws ExchangeException {
        
        return apiRetrySendRequestGetParsedResponse(PREMIUM_INDEX_REQ);
    }
    
    private JsonElement getPositionInformation() throws ExchangeException {
        
        return apiGetSignedRequestResponse(ENDPOINT_POSITION_INFO);
    }
    
    private JsonElement getAccountInformation() throws ExchangeException {
        
        return apiGetSignedRequestResponse(ENDPOINT_ACCOUNT_INFO);
    }
    
    private JsonElement getExchangeInfo() throws ExchangeException {
        
        return apiRetrySendRequestGetParsedResponse(EXCHANGE_INFO_REQ);
    }
    
    private JsonElement marketOpenHedgeMode(String symbol, boolean isLong, double symbolQty) throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=BUY&positionSide=LONG" : "&side=SELL&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow=" + RECV_WINDOW
                + "&timestamp=" + timestamp;
        
        return apiPostSignedRequestGetResponse(ENDPOINT_NEW_ORDER, params);
    }
    
    private JsonElement marketCloseHedgeMode(String symbol, boolean isLong, double symbolQty) throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=SELL&positionSide=LONG" : "&side=BUY&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow=" + RECV_WINDOW
                + "&timestamp=" + timestamp;
        
        return apiPostSignedRequestGetResponse(ENDPOINT_NEW_ORDER, params);
    }
    
    private JsonElement apiGetSignedRequestResponse(String endpoint) throws ExchangeException {
        
        long timestamp = getCurrentTimestampMillis();
        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String req = "GET " + endpoint + "?" + params + "&signature=" + signature + " HTTP/1.1\r\nConnection: close\r\nHost: "
                + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
        
        return apiRetrySendRequestGetParsedResponse(req);
    }
    
    private JsonElement apiPostSignedRequestGetResponse(String endpoint, String params) throws ExchangeException {
        
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String req = "POST " + endpoint + "?" + params + "&signature=" + signature + " HTTP/1.1\r\nConnection: close\r\nHost: "
                + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
        
        return apiRetrySendRequestGetParsedResponse(req);
    }
    
    private JsonElement apiRetrySendRequestGetParsedResponse(String request) throws ExchangeException {
        
        JsonElement parsedResponse = null;
        ExchangeException exception = new ExchangeException(-11, "apiRetrySendRequestGetParsedResponse", "Problem with retry");
        boolean canReturn = false;
        
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
                String res = apiSendRequestGetResponse(request);
                parsedResponse = parseStringAndCheckErrors(res);
                canReturn = true;
                break;
            } catch (Exception e) {
                if (e instanceof ExchangeException) {
                    exception = (ExchangeException) e;
                } else {
                    exception = new ExchangeException(-12, "apiRetrySendRequestGetParsedResponse", e.toString());
                }
                try {
                    Thread.sleep(RETRY_TIME_MILLIS);
                } catch (InterruptedException e1) {
                    logger.log(2, -16, "apiRetrySendRequestGetParsedResponse", e1.toString());
                }
            }
        }
        if (canReturn) {
            return parsedResponse;
        } else {
            throw exception;
        }
    }
    
    private String apiSendRequestGetResponse(String request) throws IOException {
        
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
                throw new ExchangeException(httpStatusCode, "parseStringAndCheckErrors",
                        "JSON account information response was null or too short");
            }
            JsonElement elem = JsonParser.parseString(jsonString);
            if (elem.isJsonObject()) {
                JsonObject objElem = elem.getAsJsonObject();
                if (objElem.has("code") && objElem.has("msg")) {
                    int code = objElem.get("code").getAsInt();
                    if (code < NO_ERROR_CODE_LOWER_BOUND || code > NO_ERROR_CODE_UPPER_BOUND) {
                        throw new ExchangeException(code, objElem.get("msg").getAsString(), "parseStringAndCheckErrors");
                    }
                }
            }
            return elem;
        } catch (Exception e) {
            if (e instanceof ExchangeException) {
                throw e;
            } else {
                throw new ExchangeException(-14, "parseStringAndCheckErrors", e.toString());
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
                        ipLimitWeight1M = (ipLimitWeight1M * 10) + inChar - 48;
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
        
        if (ipLimitWeight1M >= maxIpLimitWeight1M - LIMIT_ROOM || httpStatusCode == 403 || httpStatusCode == 429
                || httpStatusCode == 418) {
            try {
                logger.log(3, -17, "limits reached",
                        "ipLimitWeight1M: " + ipLimitWeight1M + "; httpStatusCode: " + httpStatusCode);
                Thread.sleep(LIMIT_HIT_SLEEP_TIME);
            } catch (InterruptedException e) {
                logger.log(2, -16, "getHTTPResponse", e.toString());
            }
        }
        return res.toString();
    }
    
    private long getCurrentTimestampMillis() {
        
        return System.currentTimeMillis() - TIMESTAMP_LAG;
    }
}
