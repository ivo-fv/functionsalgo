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

public class BPAPIWrapper {

    private static final String HOST_TEST = "https://testnet.binancefuture.com";
    private static final String HOST_LIVE = "https://fapi.binance.com";

    private static final long ACCOUNT_INFO_CACHE_TIME = 5000;
    private static final long RECV_WINDOW = 15000;
    private static final long TIMESTAMP_LAG = 5000;
    private static final int NO_ERROR_CODE_LOWER_BOUND = 0;
    private static final int NO_ERROR_CODE_UPPER_BOUND = 299;
    private static final int NUM_RETRIES = 4;
    private static final long RETRY_TIME_MILLIS = 200;
    private static final long LIMIT_HIT_SLEEP_TIME = 60000;
    private static final String HEADER_API_KEY = "X-MBX-APIKEY";
    private static final int CONN_TIMEOUT_MILLIS = 10000;
    private static final int BACK_OFF_NO_SPAM_CODE = 429;
    private static final int IP_BAN_CODE = 418;

    private Mac signHMAC;
    private String apiKey;
    private Logger logger;
    private String host;
    private CloseableHttpClient httpClient;

    private JsonElement accountInformation;
    private long accountInformationCacheTime = 0;

    // TODO security -> use char[] privateKey and overwrite it
    public BPAPIWrapper(Logger logger, String privateKey, String apiKey, boolean isTest)
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

    public double getAccInfTotalInitialMargin(boolean getMostUpdated) throws ExchangeException {

        return getCachedAccountInformation(getMostUpdated).getAsJsonObject().get("totalInitialMargin").getAsDouble();
    }

    public double getAccInfMarginBalance(boolean getMostUpdated) throws ExchangeException {

        return getCachedAccountInformation(getMostUpdated).getAsJsonObject().get("totalMarginBalance").getAsDouble();
    }

    public double getAccInfWalletBalance(boolean getMostUpdated) throws ExchangeException {

        return getCachedAccountInformation(getMostUpdated).getAsJsonObject().get("totalWalletBalance").getAsDouble();
    }

    public Map<String, Integer> getAccInfLeverages(boolean getMostUpdated) throws ExchangeException {

        JsonObject objAccInfo = getCachedAccountInformation(getMostUpdated).getAsJsonObject();

        Map<String, Integer> leverages = new HashMap<>();

        JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();
            leverages.put(elemObj.get("symbol").getAsString(), elemObj.get("leverage").getAsInt());
        }

        return leverages;
    }

    public Map<String, Boolean> getAccInfIsolatedSymbols(boolean getMostUpdated) throws ExchangeException {

        JsonObject objAccInfo = getCachedAccountInformation(getMostUpdated).getAsJsonObject();

        Map<String, Boolean> isSymbolIsolated = new HashMap<>();

        JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();
            isSymbolIsolated.put(elemObj.get("symbol").getAsString(), elemObj.get("isolated").getAsBoolean());
        }

        return isSymbolIsolated;
    }

    public boolean isAccInfHedgeMode(boolean getMostUpdated) throws ExchangeException {

        JsonObject objAccInfo = getCachedAccountInformation(getMostUpdated).getAsJsonObject();

        JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
        for (JsonElement elem : arrPositions) {
            JsonObject elemObj = elem.getAsJsonObject();
            return elemObj.get("positionSide").getAsString().equals("BOTH");
        }

        return false;
    }

    private JsonElement getCachedAccountInformation(boolean bypassCache) throws ExchangeException {

        if (!bypassCache && accountInformationCacheTime > System.currentTimeMillis()) {
            return accountInformation;
        }

        accountInformationCacheTime = System.currentTimeMillis() + ACCOUNT_INFO_CACHE_TIME;
        accountInformation = getAccountInformation();

        return accountInformation;
    }

    private JsonElement getAccountInformation() throws ExchangeException {

        String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + getCurrentTimestampMillis();
        String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
        String uri = host + "/fapi/v2/account?" + params + "&signature=" + signature;

        HttpGet httpget = new HttpGet(uri);
        httpget.addHeader(HEADER_API_KEY, apiKey);

        return retrySendRequestGetParsedResponse(httpget);
    }

    private JsonElement retrySendRequestGetParsedResponse(HttpUriRequest request) throws ExchangeException {

        ExchangeException exception = new ExchangeException(-1, "Retry problem", ExchangeException.NOT_FIXABLE);

        for (int i = 0; i < NUM_RETRIES; i++) {
            try (CloseableHttpResponse res = httpClient.execute(request)) {
                HttpEntity entity = res.getEntity();

                updateLimits(res);

                String json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                return parseStringAndCheckErrors(json);

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

            Utils.sleep(LIMIT_HIT_SLEEP_TIME, logger);
        }
    }

    private JsonElement parseStringAndCheckErrors(String jsonString) throws ExchangeException {

        try {
            if (jsonString == null || jsonString.length() < 2) {
                throw new ExchangeException(-1, null, ExchangeException.NOT_FIXABLE);
            }
            JsonElement elem = JsonParser.parseString(jsonString);
            if (elem.isJsonObject()) {
                JsonObject objElem = elem.getAsJsonObject();
                if (objElem.has("code") && objElem.has("msg")) {
                    int code = objElem.get("code").getAsInt();
                    if (code < NO_ERROR_CODE_LOWER_BOUND || code > NO_ERROR_CODE_UPPER_BOUND) {
                        throw new ExchangeException(code, objElem.get("msg").getAsString(),
                                ExchangeException.API_ERROR);
                    }
                }
            }
            return elem;
        } catch (ExchangeException e) {
            throw e;
        } catch (JsonParseException e) {
            throw new ExchangeException(-1, e.toString(), ExchangeException.PARSING_PROBLEM);
        }
    }

    private long getCurrentTimestampMillis() {

        return System.currentTimeMillis() - TIMESTAMP_LAG;
    }

}
