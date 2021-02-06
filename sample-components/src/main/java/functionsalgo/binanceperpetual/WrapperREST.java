package functionsalgo.binanceperpetual;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import functionsalgo.datapoints.Interval;
import functionsalgo.shared.MultiplePageJSONInOneFileSaver;
import functionsalgo.shared.Utils;

// TODO  no  magic numbers, make them constants
public class WrapperREST {

    public static final String HOST_TEST = "https://testnet.binancefuture.com";
    public static final String HOST_LIVE = "https://fapi.binance.com";

    private static final String EXCHANGE_INFO_ENDPOINT = "/fapi/v1/exchangeInfo";
    private static final String ACCOUNT_INFO_ENDPOINT = "/fapi/v2/account?";
    private static final String ORDER_ENDPOINT = "/fapi/v1/order?";
    private static final String SET_CROSS_MARGIN_ENDPOINT = "/fapi/v1/marginType?";
    private static final String SET_LEVERAGE_ENDPOINT = "/fapi/v1/leverage?";
    private static final String SET_HEDGE_MODE_ENDPOINT = "/fapi/v1/positionSide/dual?";
    private static final String ORDER_BOOK_ENDPOINT = "/fapi/v1/depth?";
    private static final String FUNDING_RATE_ENDPOINT = "/fapi/v1/fundingRate?";
    private static final String KLINES_ENDPOINT = "/fapi/v1/klines?";

    private static final long RECV_WINDOW = 15000;
    private static final long TIMESTAMP_LAG = 5000;
    private static final int NO_ERROR_CODE_LOWER_BOUND = 0;
    private static final int NO_ERROR_CODE_UPPER_BOUND = 299;
    private static final int NUM_RETRIES = 4;
    private static final long RETRY_TIME_MILLIS = 200;
    private static final long LIMIT_HIT_SLEEP_TIME_MILLIS = 60000;
    private static final String HEADER_API_KEY = "X-MBX-APIKEY";
    private static final int CONN_TIMEOUT_MILLIS = 10000;
    private static final int BACK_OFF_NO_SPAM_CODE = 429;
    private static final int IP_BAN_CODE = 418;
    private static final String TRADING_STATUS = "TRADING";
    private static final int MAX_REQUEST_KLINES_AND_FUNDING_RATES = 1000;
    private static final int MAX_ORDERBOOK_REQUEST = 1000;

    private static final Logger logger = LogManager.getLogger();

    private Mac signHMAC;
    private String apiKey;
    private String host;
    private CloseableHttpClient httpClient;

    // TODO security -> use char[] privateKey and overwrite it
    // TODO use keys from a resource file (remove them from this constructor params)
    public WrapperREST(String privateKey, String apiKey) throws NoSuchAlgorithmException, InvalidKeyException {

        host = HOST_LIVE;
        this.apiKey = apiKey;
        signHMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec pKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        signHMAC.init(pKey);

        RequestConfig config = RequestConfig.custom().setConnectTimeout(CONN_TIMEOUT_MILLIS)
                .setConnectionRequestTimeout(CONN_TIMEOUT_MILLIS).setSocketTimeout(CONN_TIMEOUT_MILLIS).build();
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public void setToTestHost() {
        host = HOST_TEST;
    }

    public void setToLiveHost() {
        host = HOST_LIVE;
    }

    public void saveKlines(List<File> klinesSymbolsFilesJSON, List<String> symbols, Interval interval, long startTime,
            long endTime) throws IOException {
        logger.info("Saving klines");
        for (int i = 0; i < klinesSymbolsFilesJSON.size(); i++) {
            saveKlines(klinesSymbolsFilesJSON.get(i), symbols.get(i), interval, startTime, endTime);
            logger.info("Saved json of symbol: {}", symbols.get(i));
        }
        logger.info("Finished saving klines");
    }

    public void saveKlines(File klinesJSONFile, String symbol, Interval interval, long startTime, long endTime)
            throws IOException {

        saveMultiPageData(klinesJSONFile, symbol, interval, startTime, endTime, KLINES_ENDPOINT);
    }

    public void saveFundingRates(List<File> symbolsJSONFiles, List<String> symbols, long startTime, long endTime)
            throws IOException {
        logger.info("Saving funding rates");
        for (int i = 0; i < symbolsJSONFiles.size(); i++) {
            saveFundingRates(symbolsJSONFiles.get(i), symbols.get(i), startTime, endTime);
            logger.info("Saved json of symbol: {}", symbols.get(i));
        }
        logger.info("Finished saving funding rates");
    }

    public void saveFundingRates(File symbolsJSONFile, String symbol, long startTime, long endTime) throws IOException {
        saveMultiPageData(symbolsJSONFile, symbol, Interval._8h, startTime, endTime, FUNDING_RATE_ENDPOINT);
    }

    public void saveOrderBooks(List<File> symbolsJSONFiles, List<String> symbols, int bookDepth, boolean append)
            throws IOException {
        logger.info("Saving order books");
        for (int i = 0; i < symbolsJSONFiles.size(); i++) {
            saveOrderBook(symbolsJSONFiles.get(i), symbols.get(i), bookDepth, append);
            logger.info("Saved json of symbol: {}", symbols.get(i));
        }
        logger.info("Finished saving order books");
    }

    public void saveOrderBook(File orderBookJSONFile, String symbol, int bookDepth, boolean append) throws IOException {

        if (bookDepth > MAX_ORDERBOOK_REQUEST) {
            bookDepth = MAX_ORDERBOOK_REQUEST;
        }

        MultiplePageJSONInOneFileSaver jsonSaver = new MultiplePageJSONInOneFileSaver(orderBookJSONFile, append);

        String endpoint = ORDER_BOOK_ENDPOINT;
        String uri = host + endpoint + "symbol=" + symbol + "&limit=" + bookDepth;
        HttpGet httpget = new HttpGet(uri);

        try (CloseableHttpResponse res = httpClient.execute(httpget)) {
            HttpEntity entity = res.getEntity();
            updateLimits(res);
            jsonSaver.appendObject(entity.getContent());
        }

        jsonSaver.finish();
    }

    public AccountInfoWrapper getAccountInfo() throws WrapperRESTException {

        JsonElement accInfoJsonElem = getAccountInformation();
        JsonObject accInfoJsonObj = accInfoJsonElem.getAsJsonObject();

        double totalInitialMargin = accInfoJsonObj.get("totalInitialMargin").getAsDouble();
        double totalMarginBalance = accInfoJsonObj.get("totalMarginBalance").getAsDouble();
        double totalWalletBalance = accInfoJsonObj.get("totalWalletBalance").getAsDouble();

        Map<String, Integer> leverages = new HashMap<>();
        Map<String, PositionWrapper> longPositions = new HashMap<>();
        Map<String, PositionWrapper> shortPositions = new HashMap<>();
        Map<String, PositionWrapper> bothPositions = new HashMap<>();

        boolean isHedgeMode = true;

        JsonArray arrPositions = accInfoJsonObj.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();

            String symbol = elemObj.get("symbol").getAsString();
            leverages.put(symbol, elemObj.get("leverage").getAsInt());

            boolean isSymbolIsolated = elemObj.get("isolated").getAsBoolean();
            boolean isLong = elemObj.get("positionSide").getAsString().equals("LONG");
            boolean isBoth = elemObj.get("positionSide").getAsString().equals("BOTH");
            double quantity = elemObj.get("positionAmt").getAsDouble();
            double averagePrice = elemObj.get("entryPrice").getAsDouble();

            if (isBoth) {
                bothPositions.put(symbol,
                        new PositionWrapper(symbol, quantity, averagePrice, isSymbolIsolated, isBoth, isLong));
            } else if (isLong) {
                longPositions.put(symbol,
                        new PositionWrapper(symbol, quantity, averagePrice, isSymbolIsolated, isBoth, isLong));
            } else {
                shortPositions.put(symbol,
                        new PositionWrapper(symbol, quantity, averagePrice, isSymbolIsolated, isBoth, isLong));
            }

            if (isBoth) {
                isHedgeMode = false;
            }
        }

        return new AccountInfoWrapper(totalInitialMargin, totalMarginBalance, totalWalletBalance, leverages,
                longPositions, shortPositions, bothPositions, isHedgeMode);
    }

    public ExchangeInfoWrapper getExchangeInfo() throws WrapperRESTException {

        JsonElement exchangeInfoJsonElem = getExchangeInformation();
        JsonObject exchangeInfoJsonObj = exchangeInfoJsonElem.getAsJsonObject();

        long exchangeTime = exchangeInfoJsonObj.get("serverTime").getAsLong();

        Map<String, Boolean> symbolTrading = new HashMap<>();
        Map<String, Double> symbolQtyStepSize = new HashMap<>();

        JsonArray arrExchangeInfoSymbols = exchangeInfoJsonObj.get("symbols").getAsJsonArray();
        for (JsonElement elem : arrExchangeInfoSymbols) {
            JsonObject objElem = elem.getAsJsonObject();

            String symbol = objElem.get("symbol").getAsString();
            symbolTrading.put(symbol, objElem.get("status").getAsString().equals(TRADING_STATUS));

            JsonArray filter = objElem.get("filters").getAsJsonArray();
            for (JsonElement filterElem : filter) {
                JsonObject objFilter = filterElem.getAsJsonObject();
                if (objFilter.get("filterType").getAsString().equals("LOT_SIZE")) {
                    symbolQtyStepSize.put(symbol, objFilter.get("stepSize").getAsDouble());
                }
            }
        }

        return new ExchangeInfoWrapper(symbolTrading, symbolQtyStepSize, exchangeTime);
    }

    public void setToHedgeMode() throws WrapperRESTException {

        String params = "dualSidePosition=true&recvWindow=" + RECV_WINDOW + "&timestamp=" + getCurrentTimestampMillis();

        JsonElement res = apiPostSignedRequestGetResponse(SET_HEDGE_MODE_ENDPOINT, params);

        JsonObject resObj = res.getAsJsonObject();
        int code = resObj.get("code").getAsInt();
        String msg = resObj.get("msg").getAsString();
        if (code != 200 && code != -4059) {
            throw new WrapperRESTException(code, msg, "WrapperREST::setToHedgeMode");
        }
    }

    public void setLeverage(String symbol, int leverage) throws WrapperRESTException {

        String params = "symbol=" + symbol + "&leverage=" + leverage + "&recvWindow=" + RECV_WINDOW + "&timestamp="
                + getCurrentTimestampMillis();

        JsonElement res = apiPostSignedRequestGetResponse(SET_LEVERAGE_ENDPOINT, params);

        JsonObject resObj = res.getAsJsonObject();
        if (resObj.has("leverage")) {
            if (resObj.get("leverage").getAsInt() != leverage) {
                throw new WrapperRESTException(WrapperRESTException.ErrorType.BAD_LEVERAGE, "incorrect leverage",
                        "WrapperREST::setLeverage");
            }
        } else if (resObj.has("code")) {
            throw new WrapperRESTException(resObj.get("code").getAsInt(), resObj.get("msg").getAsString(),
                    "WrapperREST::setLeverage");
        } else {
            throw new WrapperRESTException(WrapperRESTException.ErrorType.UNKNOWN_RESPONSE, resObj.toString(),
                    "WrapperREST::setLeverage");
        }
    }

    public void setToCrossMargin(String symbol) throws WrapperRESTException {

        String params = "symbol=" + symbol + "&marginType=CROSSED&recvWindow=" + RECV_WINDOW + "&timestamp="
                + getCurrentTimestampMillis();

        JsonElement res = apiPostSignedRequestGetResponse(SET_CROSS_MARGIN_ENDPOINT, params);

        JsonObject resObj = res.getAsJsonObject();
        int code = resObj.get("code").getAsInt();
        String msg = resObj.get("msg").getAsString();
        if (code != 200 && code != -4046) {
            throw new WrapperRESTException(code, msg, "WrapperREST::setToCrossMargin");
        }
    }

    public OrderResultWrapper marketOpenHedgeMode(String symbol, boolean isLong, double symbolQty)
            throws WrapperRESTException {

        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=BUY&positionSide=LONG" : "&side=SELL&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow="
                + RECV_WINDOW + "&timestamp=" + timestamp;

        JsonElement res = apiPostSignedRequestGetResponse(ORDER_ENDPOINT, params);

        JsonObject resObj = res.getAsJsonObject();
        if (resObj.has("symbol")) {
            return new OrderResultWrapper(resObj.get("symbol").getAsString());
        } else if (resObj.has("code")) {
            throw new WrapperRESTException(resObj.get("code").getAsInt(), resObj.get("msg").getAsString(),
                    "WrapperREST::marketOpenHedgeMode");
        } else {
            throw new WrapperRESTException(WrapperRESTException.ErrorType.UNKNOWN_RESPONSE, resObj.toString(),
                    "WrapperREST::marketOpenHedgeMode");
        }
    }

    public OrderResultWrapper marketCloseHedgeMode(String symbol, boolean isLong, double symbolQty)
            throws WrapperRESTException {

        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=SELL&positionSide=LONG" : "&side=BUY&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow="
                + RECV_WINDOW + "&timestamp=" + timestamp;

        JsonElement res = apiPostSignedRequestGetResponse(ORDER_ENDPOINT, params);

        JsonObject resObj = res.getAsJsonObject();
        if (resObj.has("symbol")) {
            return new OrderResultWrapper(resObj.get("symbol").getAsString());
        } else if (resObj.has("code")) {
            throw new WrapperRESTException(resObj.get("code").getAsInt(), resObj.get("msg").getAsString(),
                    "WrapperREST::marketCloseHedgeMode");
        } else {
            throw new WrapperRESTException(WrapperRESTException.ErrorType.UNKNOWN_RESPONSE, resObj.toString(),
                    "WrapperREST::marketCloseHedgeMode");
        }
    }

    private JsonElement apiPostSignedRequestGetResponse(String endpoint, String params) throws WrapperRESTException {

        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String uri = host + endpoint + params + "&signature=" + signature;

        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader(HEADER_API_KEY, apiKey);

        return retrySendRequestGetParsedResponse(httpPost);
    }

    private JsonElement getAccountInformation() throws WrapperRESTException {

        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + getCurrentTimestampMillis();
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String uri = host + ACCOUNT_INFO_ENDPOINT + params + "&signature=" + signature;

        HttpGet httpget = new HttpGet(uri);
        httpget.addHeader(HEADER_API_KEY, apiKey);

        JsonElement res = retrySendRequestGetParsedResponse(httpget);
        checkErrors(res);
        return res;
    }

    private JsonElement getExchangeInformation() throws WrapperRESTException {

        String uri = host + EXCHANGE_INFO_ENDPOINT;
        HttpGet httpget = new HttpGet(uri);

        JsonElement res = retrySendRequestGetParsedResponse(httpget);
        checkErrors(res);
        return res;
    }

    private void checkErrors(JsonElement json) throws WrapperRESTException {
        if (json.isJsonObject()) {
            JsonObject objElem = json.getAsJsonObject();
            if (objElem.has("code") && objElem.has("msg")) {
                int code = objElem.get("code").getAsInt();
                if (code < NO_ERROR_CODE_LOWER_BOUND || code > NO_ERROR_CODE_UPPER_BOUND) {
                    throw new WrapperRESTException(code, objElem.get("msg").getAsString(), "WrapperREST::checkErrors");
                }
            }
        }
    }

    private JsonElement retrySendRequestGetParsedResponse(HttpUriRequest request) throws WrapperRESTException {

        WrapperRESTException exception = new WrapperRESTException(WrapperRESTException.ErrorType.UNKNOWN_ERROR,
                "Retry problem", "WrapperREST::retrySendRequestGetParsedResponse");

        for (int i = 0; i < NUM_RETRIES; i++) {
            try (CloseableHttpResponse res = httpClient.execute(request)) {
                HttpEntity entity = res.getEntity();

                updateLimits(res);

                String json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                return parseJsonString(json);

            } catch (WrapperRESTException e) {
                exception = e;
            } catch (ParseException e) {
                exception = new WrapperRESTException(WrapperRESTException.ErrorType.PARSE_ERROR, e.toString(),
                        "WrapperREST::retrySendRequestGetParsedResponse");
            } catch (IOException e) {
                exception = new WrapperRESTException(WrapperRESTException.ErrorType.CONNECTION_ERROR, e.toString(),
                        "WrapperREST::retrySendRequestGetParsedResponse");
            }

            Utils.sleep(RETRY_TIME_MILLIS);
        }

        throw exception;
    }

    private void updateLimits(CloseableHttpResponse res) {

        int httpStatusCode = res.getStatusLine().getStatusCode();

        if (httpStatusCode == BACK_OFF_NO_SPAM_CODE || httpStatusCode == IP_BAN_CODE) {
            logger.error("updateLimits: limits reached  |  headers: {}  |  httpStatusCode: {}", res.getAllHeaders(),
                    httpStatusCode);

            Utils.sleep(LIMIT_HIT_SLEEP_TIME_MILLIS);
        }
    }

    private JsonElement parseJsonString(String jsonString) throws WrapperRESTException {

        try {
            if (jsonString == null || jsonString.length() < 2) {
                throw new WrapperRESTException(WrapperRESTException.ErrorType.BAD_RESPONSE_TO_PARSE,
                        "json string too small or null", "WrapperREST::parseJsonString");
            }
            return JsonParser.parseString(jsonString);
        } catch (JsonParseException e) {
            throw new WrapperRESTException(WrapperRESTException.ErrorType.PARSE_ERROR, e.toString(),
                    "WrapperREST::parseJsonString");
        }
    }

    private void saveMultiPageData(File fileToSaveTo, String symbol, Interval interval, long startTime, long endTime,
            String endpoint) throws IOException {

        long timeToAdd = (long) MAX_REQUEST_KLINES_AND_FUNDING_RATES * interval.toMilliseconds();
        long newTime = startTime;

        MultiplePageJSONInOneFileSaver jsonPagesSaver = new MultiplePageJSONInOneFileSaver(fileToSaveTo, false);

        while (newTime < endTime) {

            String uri = host + endpoint + "symbol=" + symbol + "&interval=" + interval + "&startTime=" + newTime
                    + "&endTime=" + endTime + "&limit=" + MAX_REQUEST_KLINES_AND_FUNDING_RATES;
            HttpGet httpget = new HttpGet(uri);

            try (CloseableHttpResponse res = httpClient.execute(httpget)) {
                HttpEntity entity = res.getEntity();
                updateLimits(res);
                jsonPagesSaver.appendArray(entity.getContent());
            }

            newTime += timeToAdd;
        }
        jsonPagesSaver.finish();
    }

    private long getCurrentTimestampMillis() {

        return System.currentTimeMillis() - TIMESTAMP_LAG;
    }
}
