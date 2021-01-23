package functionalgo.binanceperpetual.exchange;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import functionalgo.Logger;
import functionalgo.Utils;
import functionalgo.aws.LambdaLogger;
import functionalgo.binanceperpetual.BPLimitedAPIHandler;
import functionalgo.exceptions.ExchangeException;

// TODO refactor json parsing to more individual methods
// TODO choose and use a proper http client library
// TODO should only do stuff like margin checks, order size and price validation, and populate accinfo and
// batch execute orders
// TODO position consistency
public class BPLiveExchange implements BPExchange {

    /**
     * Used for quick testing
     */
    public static void main(String[] args) throws ExchangeException {

        String privateKey = "***REMOVED***";
        String apiKey = "***REMOVED***";

        Logger logger = new LambdaLogger(true);
        BPLimitedAPIHandler apiHandler = new BPLimitedAPIHandler(logger);

        BPExchange exchange = new BPLiveExchange(logger, apiHandler, privateKey, apiKey);

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

    private static final String HOST = BPLimitedAPIHandler.HOST;
    private static final String EXCHANGE_INFO_REQ = BPLimitedAPIHandler.EXCHANGE_INFO_REQ;
    private static final String PREMIUM_INDEX_REQ = "GET /fapi/v1/premiumIndex HTTP/1.1\r\nConnection: close\r\nHost: "
            + HOST + "\r\n\r\n";
    private static final String AUTH = "X-MBX-APIKEY: ";
    private static final long TIMESTAMP_LAG = 5000;
    private static final long RECV_WINDOW = 15000;
    private static final String TRADING_STATUS = "TRADING";
    private static final double OPEN_LOSS = 1.03;
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
    private Logger logger;
    private BPLimitedAPIHandler apiHandler;

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

    public BPLiveExchange(Logger logger, BPLimitedAPIHandler apiHandler, String privateKey, String apiKey)
            throws ExchangeException {

        try {
            this.apiKey = apiKey;
            this.logger = logger;
            this.apiHandler = apiHandler;
            signHMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec pKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            signHMAC.init(pKey);

            batchedMarketOrders = new ArrayList<>();

        } catch (Exception e) {
            logger.log(5, -1, e.toString(), Arrays.toString(e.getStackTrace()));
            throw new ExchangeException(-1, e.toString(), ExchangeException.NOT_FIXABLE);
        }
    }

    @Override
    public BPAccount getAccountInfo(long timestamp) throws ExchangeException {

        accountInfo = new BPLiveAccount();

        try {
            populateAccountBalances();

            populateAccountPositions();

            populateAccountMarkPriceFunding();

        } catch (Exception e) {
            logger.log(5, -1, e.toString(), Arrays.toString(e.getStackTrace()));
            if (e instanceof ExchangeException) {
                throw (ExchangeException) e;
            } else {
                throw new ExchangeException(-1, e.toString(), ExchangeException.NOT_FIXABLE);
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
            logger.log(0, 0, "BPLiveExchange:setHedgeMode", "OK");
        }
    }

    @Override
    public void setLeverage(String symbol, int leverage) throws ExchangeException {

        long timestamp = getCurrentTimestampMillis();
        String params = "symbol=" + symbol + "&leverage=" + leverage + "&recvWindow=" + RECV_WINDOW + "&timestamp="
                + timestamp;

        apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_LEVERAGE, params);

        if (accountInfo != null) {
            accountInfo.leverages.put(symbol, leverage);
            logger.log(0, 0, "BPLiveExchange:setLeverage: " + symbol + ";" + leverage, "OK");
        }
    }

    @Override
    public void setCrossMargin(String symbol) throws ExchangeException {

        long timestamp = getCurrentTimestampMillis();
        String params = "symbol=" + symbol + "&marginType=CROSSED&recvWindow=" + RECV_WINDOW + "&timestamp="
                + timestamp;

        apiPostSignedRequestGetResponse(ENDPOINT_CHANGE_MARGIN_TYPE, params);

        if (accountInfo != null) {
            accountInfo.isSymbolIsolated.put(symbol, false);
            logger.log(0, 0, "BPLiveExchange:setCrossMargin: " + symbol, "OK");
        }
    }

    @Override
    public void batchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty)
            throws ExchangeException {

        if (accountInfo != null) {
            accountInfo.ordersWithErrors.remove(orderId);
            batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, symbolQty, true));
        } else {
            logger.log(2, -1, "BPLiveExchange:batchMarketOpen", "Must have called getAccountInfo");
            throw new ExchangeException(-1, "Must have called getAccountInfo", ExchangeException.INVALID_STATE);
        }
    }

    @Override
    public void batchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose)
            throws ExchangeException {

        if (accountInfo != null) {
            accountInfo.ordersWithErrors.remove(orderId);
            batchedMarketOrders.add(new BatchedOrder(orderId, symbol, isLong, qtyToClose, false));
        } else {
            logger.log(2, -1, "BPLiveExchange:batchMarketClose", "Must have called getAccountInfo");
            throw new ExchangeException(-1, "Must have called getAccountInfo", ExchangeException.INVALID_STATE);
        }
    }

    @Override
    public BPAccount executeBatchedOrders() throws ExchangeException {

        // remove batch from list to execute whether or not it was successful
        ArrayList<BatchedOrder> tempBatch = new ArrayList<>(batchedMarketOrders);
        batchedMarketOrders.clear();

        // adjust the order quantity to the step size and check if there's enough margin
        // to execute the orders
        double sumInitialMargin = accountInfo.totalInitialMargin;
        try {
            JsonElement exchangeInfo = getExchangeInfo();
            JsonObject objExchangeInfo = exchangeInfo.getAsJsonObject();
            JsonArray arrExchangeInfoSymbols = objExchangeInfo.get("symbols").getAsJsonArray();
            boolean found = false;
            for (BatchedOrder order : tempBatch) {
                for (JsonElement elem : arrExchangeInfoSymbols) {
                    JsonObject objElem = elem.getAsJsonObject();
                    // from here
                    if (objElem.get("symbol").getAsString().equals(order.symbol)
                            && objElem.get("status").getAsString().equals(TRADING_STATUS)) {
                        JsonArray filter = objElem.get("filters").getAsJsonArray();
                        for (JsonElement filterElem : filter) {
                            JsonObject objFilter = filterElem.getAsJsonObject();
                            if (objFilter.get("filterType").getAsString().equals("LOT_SIZE")) {
                                double stepSize = objFilter.get("stepSize").getAsDouble();
                                if (order.quantity < stepSize) {
                                    throw new ExchangeException(-1, order.symbol, ExchangeException.PARAM_PROBLEM);
                                }
                                order.quantity = BigDecimal.valueOf(Math.floor(order.quantity / stepSize))
                                        .multiply(BigDecimal.valueOf(stepSize)).doubleValue();
                                // to here -> checkIfOrderIsValid on batchMarketOpen
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
                    throw new ExchangeException(-1,
                            "Could not find expected JSON members in exchangeInfo: BPLiveExchange:executeBatchedOrders",
                            ExchangeException.PARSING_PROBLEM);
                } else {
                    found = false;
                }
            }
        } catch (Exception e) {
            if (e instanceof ExchangeException) {
                logger.log(4, ((ExchangeException) e).getCode(), e.toString(), Arrays.toString(e.getStackTrace()));
                throw e;
            } else {
                logger.log(4, -1, e.toString(), Arrays.toString(e.getStackTrace()));
                throw new ExchangeException(-1, e.toString(), ExchangeException.NOT_FIXABLE);
            }
        }

        if (sumInitialMargin >= accountInfo.marginBalance) {
            logger.log(2, -1, "BPLiveExchange:executeBatchedOrders", "Not enough margin to open");
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
                                logger.log(0, 0, "BPLiveExchange:executeBatchedOrders", "Executed: OPEN " + order.symbol
                                        + (order.isLong ? " LONG " : " SHORT ") + order.quantity);
                            } else {
                                throw new ExchangeException(-1, "returned json doesn't have expected members",
                                        ExchangeException.PARSING_PROBLEM);
                            }
                        } else {
                            throw new ExchangeException(-1,
                                    "Only hedge mode orders supported: BPLiveExchange:executeBatchedOrders",
                                    ExchangeException.INVALID_STATE);
                        }
                    } catch (Exception e) {
                        if (e instanceof ExchangeException) {
                            accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                            logger.log(3, ((ExchangeException) e).getCode(), e.toString(),
                                    Arrays.toString(e.getStackTrace()));
                        } else {
                            accountInfo.ordersWithErrors.put(order.orderId,
                                    new ExchangeException(-1, e.toString(), ExchangeException.ORDER_FAILED));
                            logger.log(3, -1, e.toString(), Arrays.toString(e.getStackTrace()));

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
                            logger.log(0, 0, "BPLiveExchange:executeBatchedOrders", "Executed: CLOSE" + order.symbol
                                    + (order.isLong ? " LONG " : " SHORT ") + order.quantity);
                        } else {
                            throw new ExchangeException(-1, "returned json doesn't have expected members",
                                    ExchangeException.PARSING_PROBLEM);
                        }
                    } else {
                        throw new ExchangeException(-1,
                                "Only hedge mode orders supported: BPLiveExchange:executeBatchedOrders",
                                ExchangeException.INVALID_STATE);
                    }
                } catch (Exception e) {
                    if (e instanceof ExchangeException) {
                        accountInfo.ordersWithErrors.put(order.orderId, (ExchangeException) e);
                        logger.log(3, ((ExchangeException) e).getCode(), e.toString(),
                                Arrays.toString(e.getStackTrace()));
                    } else {
                        accountInfo.ordersWithErrors.put(order.orderId,
                                new ExchangeException(-1, e.toString(), ExchangeException.ORDER_FAILED));
                        logger.log(3, -1, e.toString(), Arrays.toString(e.getStackTrace()));
                    }
                }
            }
        }

        try {
            updateAccountBalances(); // TODO use /fapi/v2/account for this and accpositions...->
        } catch (ExchangeException e) {
            accountInfo.isBalancesDesynch = true;
            logger.log(4, e.getCode(), e.toString(), Arrays.toString(e.getStackTrace()));
        }

        try {
            populateAccountPositions(); // TODO use /fapi/v2/account for this and accbalances...->
        } catch (ExchangeException e) {
            accountInfo.isPositionsDesynch = true;
            logger.log(4, e.getCode(), e.toString(), Arrays.toString(e.getStackTrace()));
        }

        // ...-> so calling one of the AccInfo methods in the wrapper with
        // mostUpTodate=true and the rest =false

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
            accountInfo.isSymbolIsolated.put(elemObj.get("symbol").getAsString(),
                    elemObj.get("isolated").getAsBoolean());
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
            double quantity = objElem.get("positionAmt").getAsDouble(); // not needed /fapi/v2/account gives positionAmt
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
                    throw new ExchangeException(-1,
                            "JSON position information symbol was not LONG, SHORT or BOTH: BPLiveExchange:populateAccountPositions",
                            ExchangeException.PARSING_PROBLEM);
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
                accountInfo.totalInitialMargin = accountInfo.marginBalance
                        - objElem.get("availableBalance").getAsDouble();
                accountInfo.isBalancesDesynch = false;
                break;
            }
        }
    }

    private JsonElement getAccountBalance() throws ExchangeException {

        return apiGetSignedRequestResponse(ENDPOINT_ACCOUNT_BALANCE);
    }

    private JsonElement getPremiumIndex() throws ExchangeException {

        return apiHandler.apiRetrySendRequestGetParsedResponse(PREMIUM_INDEX_REQ);
    }

    private JsonElement getPositionInformation() throws ExchangeException {

        return apiGetSignedRequestResponse(ENDPOINT_POSITION_INFO);
    }

    private JsonElement getAccountInformation() throws ExchangeException {

        return apiGetSignedRequestResponse(ENDPOINT_ACCOUNT_INFO);
    }

    private JsonElement getExchangeInfo() throws ExchangeException {

        return apiHandler.apiRetrySendRequestGetParsedResponse(EXCHANGE_INFO_REQ);
    }

    private JsonElement marketOpenHedgeMode(String symbol, boolean isLong, double symbolQty) throws ExchangeException {

        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=BUY&positionSide=LONG" : "&side=SELL&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow="
                + RECV_WINDOW + "&timestamp=" + timestamp;

        return apiPostSignedRequestGetResponse(ENDPOINT_NEW_ORDER, params);
    }

    private JsonElement marketCloseHedgeMode(String symbol, boolean isLong, double symbolQty) throws ExchangeException {

        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=SELL&positionSide=LONG" : "&side=BUY&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow="
                + RECV_WINDOW + "&timestamp=" + timestamp;

        return apiPostSignedRequestGetResponse(ENDPOINT_NEW_ORDER, params);
    }

    private JsonElement apiGetSignedRequestResponse(String endpoint) throws ExchangeException {

        long timestamp = getCurrentTimestampMillis();
        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String req = "GET " + endpoint + "?" + params + "&signature=" + signature
                + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";

        return apiHandler.apiRetrySendRequestGetParsedResponse(req);
    }

    private JsonElement apiPostSignedRequestGetResponse(String endpoint, String params) throws ExchangeException {

        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String req = "POST " + endpoint + "?" + params + "&signature=" + signature
                + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";

        return apiHandler.apiRetrySendRequestGetParsedResponse(req);
    }

    private long getCurrentTimestampMillis() {

        return System.currentTimeMillis() - TIMESTAMP_LAG;
    }
}
