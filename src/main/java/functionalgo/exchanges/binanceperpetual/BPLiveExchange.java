package functionalgo.exchanges.binanceperpetual;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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
    
    // TODO when instantiate set leverages to init and hedge mode and cross mode
    // TODO log every transaction, failures and issue
    // TODO make service methods to manually correct problems such as if the store was wiped
    // TODO when logging include json error message and http code
    // TODO include a logger as parameter to livexchange constructor
    public static void main(String[] args) throws IOException {
        
        String privateKey = "***REMOVED***";
        String apiKey = "***REMOVED***";
        BPLiveStore store = new BPLiveFileAWSStore();
        BPExchange exchange = new BPLiveExchange(store, privateKey, apiKey);
        
        System.out.println("Sending test order...");
        
        // TODO test with odd quantities like 1.0575
        // exchange.marketOpen("test_1_L", "ETHUSDT", true, 1);
        // exchange.marketOpen("test_1_L", "ETHUSDT", true, 1);
        
        exchange.getAccountInfo(System.currentTimeMillis());
        /*exchange.batchMarketOpen("test_1_L", "ETHUSDT", true, 1.34562);
        exchange.batchMarketOpen("test_1_S", "BTCUSDT", false, 0.12345);
        exchange.executeBatchedOrders();*/
        
        // TODO Auto-generated method stub
    }
    
    private static final byte[] IP_LIMIT_HEADER_WEIGHT_1M = "X-MBX-USED-WEIGHT-1M: ".getBytes(StandardCharsets.UTF_8);
    private static final int RES_BUFF_SIZE = 102400;
    
    private static final String HOST = "testnet.binancefuture.com";
    private static final String EXCHANGE_INFO_REQ = "GET /fapi/v1/exchangeInfo HTTP/1.1\r\nConnection: close\r\nHost: " + HOST
            + "\r\n\r\n";
    private static final String AUTH = "X-MBX-APIKEY: ";
    private static final long TIMESTAMP_LAG = 5000;
    private static final long RECV_WINDOW = 15000;
    
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
        
        double totalInitialMargin = 0;
        double marginBalance = 0;
        double walletBalance = 0;
        HashMap<String, Integer> leverages;
        
        public AccountInfo() {
            
            leverages = new HashMap<>();
        }
        
        @Override
        public double getQuantity(String positionId) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public long getTimestamp() {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getWalletBalance() {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public int getLeverage(String symbol) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getTakerFee() {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getMarginBalance() {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getAverageOpenPrice(String positionId) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getWorstMarginBalance() {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getCurrentPrice(String symbol) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public long getNextFundingTime() {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
        @Override
        public double getFundingRate(String symbol) {
            
            // TODO Auto-generated method stub
            return 0;
        }
        
    }
    
    public BPLiveExchange(BPLiveStore store, String privateKey, String apiKey) {
        
        // TODO when instantiate set leverages to init and hedge mode and cross mode
        try {
            this.store = store;
            this.apiKey = apiKey;
            
            signHMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec pKey = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            signHMAC.init(pKey);
            
            tlsSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            
            batchedMarketOrders = new ArrayList<>();
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // TODO throw custom exception
        }
    }
    
    @Override
    public BPExchangeAccountInfo getAccountInfo(long timestamp) {
        
        accountInfo = store.getAccountInfo(apiKey);
        if (accountInfo == null) {
            // TODO log this
            accountInfo = new AccountInfo();
        }
        
        try {
            String jsonAccInfo = getAccountInformation();
            JsonElement elemAccInfo = JsonParser.parseString(jsonAccInfo);
            JsonObject objAccInfo = elemAccInfo.getAsJsonObject();
            accountInfo.totalInitialMargin = objAccInfo.get("totalInitialMargin").getAsDouble();
            accountInfo.marginBalance = objAccInfo.get("totalMarginBalance").getAsDouble();
            accountInfo.walletBalance = objAccInfo.get("totalWalletBalance").getAsDouble();
            JsonArray arrPositions = objAccInfo.get("positions").getAsJsonArray();
            for (JsonElement elem : arrPositions) {
                JsonObject elemObj = elem.getAsJsonObject();
                accountInfo.leverages.put(elemObj.get("symbol").getAsString(), elemObj.get("leverage").getAsInt());
            }
        } catch (Exception e) {
            // TODO log this, if it's not IOException, it's a json parsing problem
            return null;
        }
        
        return accountInfo;
    }
    
    private String getAccountInformation() throws IOException {
        
        try (Socket socket = tlsSocketFactory.createSocket(HOST, 443)) {
            
            long timestamp = getCurrentTimestampMillis();
            String params = "recvWindow=" + RECV_WINDOW + "&timestamp=" + timestamp;
            String signature = Utils.bytesToHex(signHMAC.doFinal(params.getBytes(StandardCharsets.UTF_8)));
            String accInfoReq = "GET /fapi/v2/account?" + params + "&signature=" + signature
                    + " HTTP/1.1\r\nConnection: close\r\nHost: " + HOST + "\r\n" + AUTH + apiKey + "\r\n\r\n";
            
            OutputStream output = socket.getOutputStream();
            output.write(accInfoReq.getBytes(StandardCharsets.UTF_8));
            output.flush();
            
            return getHTTPResponse(socket.getInputStream());
        }
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
        JsonElement exchangeInfo;
        try {
            exchangeInfo = getExchangeInfo();
            //
            
        } catch (Exception e) {
            // TODO log the event
            return null;
        }
        // parse exchangeinfo in try or later and return null if exchangeinfo is null???
        
        // check if all of the orders will be executed, check necessary info with the exchangeInfo JSON element
        // if not, return null, empty batch lists regardless;
        // truncate as needed
        double sumInitialMargin = accountInfo.totalInitialMargin;
        
        // TODO get mark price on getaccinfo, throttle getaccinfo
        for (BatchedOrder order : batchedMarketOrders) {
            if (order.isOpen) {
                // order.quantity = truncate qty as needed
                // sumInitialMargin += ((order.quantity * mark price of order.symbol) / leverage) * some open loss%
                // check exchangeInfo filters like max order limit
                
            } else {
                // order.quantity = truncate qty as needed
                // sumInitialMargin -= (order.quantity * mark price of order.symbol) / leverage
            }
        }
        
        for (BatchedOrder order : batchedMarketOrders) {
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
        
        // must have acc info already, check margins, check order limits etc
        
        // TODO if successful update accountInfo, call /fapi/v2/balance to get updated balances
        // TODO if error in set, still return accountInfo, strat should not be concerned
        store.setAccountInfo(apiKey, accountInfo);
        
        return accountInfo;
    }
    
    private JsonElement getExchangeInfo() throws IOException {
        
        try (Socket socket = tlsSocketFactory.createSocket(HOST, 443)) {
            
            OutputStream output = socket.getOutputStream();
            output.write(EXCHANGE_INFO_REQ.getBytes(StandardCharsets.UTF_8));
            output.flush();
            
            return JsonParser.parseString(getHTTPResponse(socket.getInputStream()));
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
