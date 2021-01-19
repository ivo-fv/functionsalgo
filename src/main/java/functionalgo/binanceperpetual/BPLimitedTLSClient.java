package functionalgo.binanceperpetual;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.net.ssl.SSLSocketFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import functionalgo.Logger;
import functionalgo.exceptions.ExchangeException;

// TODO use an http library
public class BPLimitedTLSClient {
    
    public static final String HOST = "testnet.binancefuture.com";
    public static final String EXCHANGE_INFO_REQ = "GET /fapi/v1/exchangeInfo HTTP/1.1\r\nConnection: close\r\nHost: " + HOST
            + "\r\n\r\n";
    
    private static final byte[] IP_LIMIT_HEADER_WEIGHT_1M = "X-MBX-USED-WEIGHT-1M: ".getBytes(StandardCharsets.UTF_8);
    private static final int RES_BUFF_SIZE = 102400;
    private static final int NO_ERROR_CODE_LOWER_BOUND = 0;
    private static final int NO_ERROR_CODE_UPPER_BOUND = 299;
    private static final int NUM_RETRIES = 4;
    private static final int RETRY_TIME_MILLIS = 200;
    private static final int LIMIT_ROOM = 20;
    private static final long LIMIT_HIT_SLEEP_TIME = 60000;
    
    private SSLSocketFactory tlsSocketFactory;
    private int maxIpLimitWeight1M = Integer.MAX_VALUE;
    private int ipLimitWeight1M;
    private int httpStatusCode;
    private Logger logger;
    
    public BPLimitedTLSClient(Logger logger) throws ExchangeException {
        
        this.logger = logger;
        
        tlsSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        
        JsonElement exchangeInfo = apiRetrySendRequestGetParsedResponse(EXCHANGE_INFO_REQ);
        JsonObject objExchangeInfo = exchangeInfo.getAsJsonObject();
        JsonArray arrExchangeInfoLimits = objExchangeInfo.get("rateLimits").getAsJsonArray();
        for (JsonElement elem : arrExchangeInfoLimits) {
            JsonObject elemObj = elem.getAsJsonObject();
            if (elemObj.get("rateLimitType").getAsString().equals("REQUEST_WEIGHT")) {
                maxIpLimitWeight1M = elemObj.get("limit").getAsInt();
                break;
            }
        }
    }
    
    public JsonElement apiRetrySendRequestGetParsedResponse(String request) throws ExchangeException {
        
        JsonElement parsedResponse = null;
        ExchangeException exception = new ExchangeException(-1, "Retry problem", ExchangeException.NOT_FIXABLE);
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
                    exception = new ExchangeException(-1, e.toString(), ExchangeException.NOT_FIXABLE);
                }
                try {
                    Thread.sleep(RETRY_TIME_MILLIS);
                } catch (InterruptedException e1) {
                    logger.log(2, -1, e1.toString() + " ; " + e.toString(),
                            Arrays.toString(e1.getStackTrace()) + " ; " + Arrays.toString(e.getStackTrace()));
                }
            }
        }
        if (canReturn) {
            return parsedResponse;
        } else {
            throw exception;
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
                        throw new ExchangeException(code, objElem.get("msg").getAsString(), ExchangeException.API_ERROR);
                    }
                }
            }
            return elem;
        } catch (Exception e) {
            if (e instanceof ExchangeException) {
                throw e;
            } else {
                throw new ExchangeException(-1, e.toString(), ExchangeException.PARSING_PROBLEM);
            }
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
    
    /**
     * While parsing the htpp response the ipLimitWeight1M and httpStatusCode fields are set.
     * Quick and very dirty http parser. HttpURLConnection is closed when an error happens so getErrorStream()
     * can't be used to get error responses.
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
        // check if it's chunked
        String chunkRes = null;
        if (res.charAt(res.length() - 1) == '\n' && res.charAt(res.length() - 2) == '\r') {
            char[] buff = new char[res.length()];
            int num = -1;
            int off = 0;
            int buffoff = 0;
            for (int i = 0; i < buff.length; i++) {
                switch (num) {
                    case 0:
                        break;
                    case -1:
                        if (res.charAt(i) == '\r') {
                            num = Integer.parseInt(res.substring(off, i), 16);
                            off = i + 4 + num;
                            i++;
                        }
                        continue;
                    default:
                        for (int j = 0; j < num; j++) {
                            buff[buffoff] = res.charAt(i);
                            buffoff++;
                            i++;
                        }
                        i++;
                        num = -1;
                        continue;
                }
                break;
            }
            chunkRes = new String(buff, 0, buffoff);
        }
        
        if (ipLimitWeight1M >= maxIpLimitWeight1M - LIMIT_ROOM || httpStatusCode == 403 || httpStatusCode == 429
                || httpStatusCode == 418) {
            try {
                logger.log(3, -1, "limits reached",
                        "ipLimitWeight1M: " + ipLimitWeight1M + "; httpStatusCode: " + httpStatusCode);
                Thread.sleep(LIMIT_HIT_SLEEP_TIME);
            } catch (InterruptedException e) {
                logger.log(2, -1, e.toString(), Arrays.toString(e.getStackTrace()));
            }
        }
        
        if (chunkRes == null) {
            return res.toString();
        } else {
            return chunkRes;
        }
    }
}
