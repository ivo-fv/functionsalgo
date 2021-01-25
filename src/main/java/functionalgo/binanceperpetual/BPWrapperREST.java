package functionalgo.binanceperpetual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import functionalgo.Logger;
import functionalgo.Utils;
import functionalgo.exceptions.ExchangeException;

// TODO write tests for already implemented methods
// TODO rest of needed API methods
// TODO remove logger requirement once singleton logger implemented
// TODO  no  magic numbers, make them constants
public class BPWrapperREST {

    private static final String HOST_TEST = "https://testnet.binancefuture.com";
    private static final String HOST_LIVE = "https://fapi.binance.com";

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

    private Mac signHMAC;
    private String apiKey;
    private Logger logger;
    private String host;
    private CloseableHttpClient httpClient;

    // TODO security -> use char[] privateKey and overwrite it

    public BPWrapperREST(Logger logger, String privateKey, String apiKey, boolean isTest)
            throws NoSuchAlgorithmException, InvalidKeyException {

        if (isTest) {
            host = HOST_TEST;
        } else {
            host = HOST_LIVE;
        }

        this.apiKey = apiKey;
        this.logger = logger;
        signHMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec pKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        signHMAC.init(pKey);

        RequestConfig config = RequestConfig.custom().setConnectTimeout(CONN_TIMEOUT_MILLIS)
                .setConnectionRequestTimeout(CONN_TIMEOUT_MILLIS).setSocketTimeout(CONN_TIMEOUT_MILLIS).build();
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public BPAccountInfoWrapper getAccountInfo() throws ExchangeException {

        JsonElement accInfoJsonElem = getAccountInformation();
        JsonObject accInfoJsonObj = accInfoJsonElem.getAsJsonObject();

        double totalInitialMargin = accInfoJsonObj.get("totalInitialMargin").getAsDouble();
        double totalMarginBalance = accInfoJsonObj.get("totalMarginBalance").getAsDouble();
        double totalWalletBalance = accInfoJsonObj.get("totalWalletBalance").getAsDouble();

        Map<String, Integer> leverages = new HashMap<>();
        Map<String, Boolean> isSymbolIsolated = new HashMap<>();
        Map<String, Double> longPositions = new HashMap<>();
        Map<String, Double> shortPositions = new HashMap<>();
        Map<String, Double> bothPositions = new HashMap<>();

        JsonArray arrPositions = accInfoJsonObj.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();

            String symbol = elemObj.get("symbol").getAsString();
            leverages.put(symbol, elemObj.get("leverage").getAsInt());
            isSymbolIsolated.put(symbol, elemObj.get("isolated").getAsBoolean());

            if (elemObj.get("positionSide").getAsString().equals("LONG")) {
                longPositions.put(symbol, elemObj.get("positionAmt").getAsDouble());
            } else if (elemObj.get("positionSide").getAsString().equals("SHORT")) {
                shortPositions.put(symbol, elemObj.get("positionAmt").getAsDouble());
            } else {
                bothPositions.put(symbol, elemObj.get("positionAmt").getAsDouble());
            }
        }

        boolean isHedgeMode = bothPositions.isEmpty();

        return new BPAccountInfoWrapper(totalInitialMargin, totalMarginBalance, totalWalletBalance, leverages,
                isSymbolIsolated, isHedgeMode, longPositions, shortPositions, bothPositions);
    }

    public BPExchangeInfoWrapper getExchangeInfo() throws ExchangeException {

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

        return new BPExchangeInfoWrapper(symbolTrading, symbolQtyStepSize, exchangeTime);
    }

    public void setToHedgeMode() throws ExchangeException {

        String params = "dualSidePosition=true&recvWindow=" + RECV_WINDOW + "&timestamp=" + getCurrentTimestampMillis();

        JsonElement res = apiPostSignedRequestGetResponse("/fapi/v1/positionSide/dual?", params);

        JsonObject resObj = res.getAsJsonObject();
        int code = resObj.get("code").getAsInt();
        String msg = resObj.get("msg").getAsString();
        if (code != 200 && code != -4059) {
            throw new ExchangeException(code, msg, "BPWrapperREST::setToHedgeMode");
        }
    }

    public void setLeverage(String symbol, int leverage) throws ExchangeException {

        String params = "symbol=" + symbol + "&leverage=" + leverage + "&recvWindow=" + RECV_WINDOW + "&timestamp="
                + getCurrentTimestampMillis();

        JsonElement res = apiPostSignedRequestGetResponse("/fapi/v1/leverage?", params);

        JsonObject resObj = res.getAsJsonObject();
        if (resObj.has("leverage")) {
            if (resObj.get("leverage").getAsInt() != leverage) {
                throw new ExchangeException(-1, "incorrect leverage", "BPWrapperREST::setLeverage");
            }
        } else if (resObj.has("code")) {
            throw new ExchangeException(resObj.get("code").getAsInt(), resObj.get("msg").getAsString(),
                    "BPWrapperREST::setLeverage");
        } else {
            throw new ExchangeException(-1, resObj.toString(), "BPWrapperREST::setLeverage");
        }
    }

    public void setToCrossMargin(String symbol) throws ExchangeException {

        String params = "symbol=" + symbol + "&marginType=CROSSED&recvWindow=" + RECV_WINDOW + "&timestamp="
                + getCurrentTimestampMillis();

        JsonElement res = apiPostSignedRequestGetResponse("/fapi/v1/marginType?", params);

        JsonObject resObj = res.getAsJsonObject();
        int code = resObj.get("code").getAsInt();
        String msg = resObj.get("msg").getAsString();
        if (code != 200 && code != -4046) {
            throw new ExchangeException(code, msg, "BPWrapperREST::setToHedgeMode");
        }
    }

    public BPOrderResultWrapper marketOpenHedgeMode(String symbol, boolean isLong, double symbolQty)
            throws ExchangeException {

        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=BUY&positionSide=LONG" : "&side=SELL&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow="
                + RECV_WINDOW + "&timestamp=" + timestamp;

        JsonElement res = apiPostSignedRequestGetResponse("/fapi/v1/order?", params);

        JsonObject resObj = res.getAsJsonObject();
        if (resObj.has("symbol")) {
            return new BPOrderResultWrapper(resObj.get("symbol").getAsString());
        } else if (resObj.has("code")) {
            throw new ExchangeException(resObj.get("code").getAsInt(), resObj.get("msg").getAsString(),
                    "BPWrapperREST::marketOpenHedgeMode");
        } else {
            throw new ExchangeException(-1, resObj.toString(), "BPWrapperREST::marketOpenHedgeMode");
        }
    }

    public BPOrderResultWrapper marketCloseHedgeMode(String symbol, boolean isLong, double symbolQty)
            throws ExchangeException {

        long timestamp = getCurrentTimestampMillis();
        String sideCombo = isLong ? "&side=SELL&positionSide=LONG" : "&side=BUY&positionSide=SHORT";
        String params = "symbol=" + symbol + sideCombo + "&type=MARKET&quantity=" + symbolQty + "&recvWindow="
                + RECV_WINDOW + "&timestamp=" + timestamp;

        JsonElement res = apiPostSignedRequestGetResponse("/fapi/v1/order?", params);

        JsonObject resObj = res.getAsJsonObject();
        if (resObj.has("symbol")) {
            return new BPOrderResultWrapper(resObj.get("symbol").getAsString());
        } else if (resObj.has("code")) {
            throw new ExchangeException(resObj.get("code").getAsInt(), resObj.get("msg").getAsString(),
                    "BPWrapperREST::marketOpenHedgeMode");
        } else {
            throw new ExchangeException(-1, resObj.toString(), "BPWrapperREST::marketOpenHedgeMode");
        }
    }

    private JsonElement apiPostSignedRequestGetResponse(String endpoint, String params) throws ExchangeException {

        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String uri = host + endpoint + params + "&signature=" + signature;

        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader(HEADER_API_KEY, apiKey);

        return retrySendRequestGetParsedResponse(httpPost);
    }

    private JsonElement getAccountInformation() throws ExchangeException {

        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + getCurrentTimestampMillis();
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String uri = host + "/fapi/v2/account?" + params + "&signature=" + signature;

        HttpGet httpget = new HttpGet(uri);
        httpget.addHeader(HEADER_API_KEY, apiKey);

        JsonElement res = retrySendRequestGetParsedResponse(httpget);
        checkErrors(res);
        return res;
    }

    private JsonElement getExchangeInformation() throws ExchangeException {

        String uri = host + "/fapi/v1/exchangeInfo";
        HttpGet httpget = new HttpGet(uri);

        JsonElement res = retrySendRequestGetParsedResponse(httpget);
        checkErrors(res);
        return res;
    }

    private void checkErrors(JsonElement json) throws ExchangeException {
        if (json.isJsonObject()) {
            JsonObject objElem = json.getAsJsonObject();
            if (objElem.has("code") && objElem.has("msg")) {
                int code = objElem.get("code").getAsInt();
                if (code < NO_ERROR_CODE_LOWER_BOUND || code > NO_ERROR_CODE_UPPER_BOUND) {
                    throw new ExchangeException(code, objElem.get("msg").getAsString(), ExchangeException.API_ERROR);
                }
            }
        }
    }

    private JsonElement retrySendRequestGetParsedResponse(HttpUriRequest request) throws ExchangeException {

        ExchangeException exception = new ExchangeException(-1, "Retry problem", ExchangeException.NOT_FIXABLE);

        for (int i = 0; i < NUM_RETRIES; i++) {
            try (CloseableHttpResponse res = httpClient.execute(request)) {
                HttpEntity entity = res.getEntity();

                updateLimits(res);

                String json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                return parseJsonString(json);

            } catch (ExchangeException e) {
                exception = e;
            } catch (ParseException | IOException e) {
                exception = new ExchangeException(-1, e.toString(), ExchangeException.NOT_FIXABLE);
            }

            Utils.sleep(RETRY_TIME_MILLIS, logger);
        }

        throw exception;
    }

    private void updateLimits(CloseableHttpResponse res) {

        int httpStatusCode = res.getStatusLine().getStatusCode();

        if (httpStatusCode == BACK_OFF_NO_SPAM_CODE || httpStatusCode == IP_BAN_CODE) {

            String headersString = res.getAllHeaders().toString();

            logger.log(3, -1, "limits reached", "headers: " + headersString + "; httpStatusCode: " + httpStatusCode);

            Utils.sleep(LIMIT_HIT_SLEEP_TIME_MILLIS, logger);
        }
    }

    private JsonElement parseJsonString(String jsonString) throws ExchangeException {

        try {
            if (jsonString == null || jsonString.length() < 2) {
                throw new ExchangeException(-1, "json string too small or null", ExchangeException.NOT_FIXABLE);
            }
            return JsonParser.parseString(jsonString);
        } catch (JsonParseException e) {
            throw new ExchangeException(-1, e.toString(), ExchangeException.PARSING_PROBLEM);
        }
    }

    private long getCurrentTimestampMillis() {

        return System.currentTimeMillis() - TIMESTAMP_LAG;
    }

}
