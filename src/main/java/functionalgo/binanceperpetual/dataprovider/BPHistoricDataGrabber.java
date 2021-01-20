package functionalgo.binanceperpetual.dataprovider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;

import functionalgo.datapoints.Interval;

public class BPHistoricDataGrabber {
    
    public static final String API_FUTURES_URL = "https://fapi.binance.com";
    public static final short MAX_LIMIT = 2399;
    public static final long LIMIT_WINDOW_MILISEC = 60000; // 60000ms is 1min
    public static final String WEIGHT_HEADER = "x-mbx-used-weight-1m";
    public static final int STOP_API_SPAM_CODE = 429;
    public static final int WAF_RULE_BREAK_API_CODE = 403;
    
    public static final String KLINES_URL = "/fapi/v1/klines";
    public static final short MAX_KLINES_REQUEST = 1500;
    
    public static final String FUNDING_RATE_URL = "/fapi/v1/fundingRate";
    public static final short MAX_FUNDING_RATE_REQUEST = 1000;
    
    private static final String ORDERBOOK_URL = "/fapi/v1/depth";
    public static final short MAX_ORDERBOOK_REQUEST = 1000;
    
    public static final String[] SYMBOLS = { "BTCUSDT", "ETHUSDT", "BCHUSDT", "XRPUSDT", "EOSUSDT", "LTCUSDT", "TRXUSDT",
            "ETCUSDT", "LINKUSDT", "XLMUSDT", "ADAUSDT", "XMRUSDT", "DASHUSDT", "ZECUSDT", "XTZUSDT", "BNBUSDT", "ATOMUSDT",
            "ONTUSDT", "IOTAUSDT", "BATUSDT", "VETUSDT", "NEOUSDT", "QTUMUSDT", "IOSTUSDT" };
    
    private int currentWeightValue = -1;
    private int responseCode = -1;
    private long cronometer = -1;
    
    public BPHistoricDataGrabber() {
        
        currentWeightValue = -1;
        responseCode = -1;
        cronometer = -1;
    }
    
    /**
     * Used for pulling data from the exchange for backtesting
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        
        BPHistoricDataGrabber grabber = new BPHistoricDataGrabber();
        
        long startTime = LocalDateTime.of(2019, 4, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTime = LocalDateTime.of(2021, 4, 10, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
        
        System.out.println("Kline start");
        
        Interval interval = Interval._5m;
        
        String klinesDirName = BPHistoricKlines.getDirName(interval);
        new File(klinesDirName).mkdirs();
        
        TreeMap<String, File> symbolFile = new TreeMap<>();
        
        for (String symbol : getSymbols()) {
            
            symbolFile.put(symbol, new File(
                    klinesDirName + "/" + symbol + "_" + interval.toString() + "_" + startTime + "_" + endTime + ".json"));
        }
        
        grabber.saveMultipleSymbolKlines(symbolFile, interval, startTime, endTime);
        
        System.out.println("Kline end");
        
        System.out.println("Funding rate start");
        
        String fundRatesFileName = BPHistoricFundingRates.getDirName();
        new File(fundRatesFileName).mkdirs();
        
        TreeMap<String, File> symbolFratesFile = new TreeMap<>();
        
        for (String symbol : getSymbols()) {
            
            symbolFratesFile.put(symbol,
                    new File(fundRatesFileName + "/" + symbol + "_FundingRate_" + startTime + "_" + endTime + ".json"));
        }
        
        grabber.saveMultipleSymbolFundingRates(symbolFratesFile, startTime, endTime);
        
        System.out.println("Funding rate end");
    }
    
    public static String[] getSymbols() {
        
        return SYMBOLS;
    }
    
    /**
     * Similar to saveMultipleSymbolKlines but for funding rates
     * 
     * @param symbolFile
     * @param startTime
     * @param endTime
     * @throws IOException
     * @throws InterruptedException
     */
    public void saveMultipleSymbolFundingRates(Map<String, File> symbolFile, long startTime, long endTime)
            throws IOException, InterruptedException {
        
        for (Map.Entry<String, File> entry : symbolFile.entrySet()) {
            
            saveSymbolFundingRates(entry.getKey(), startTime, endTime, entry.getValue());
        }
    }
    
    /**
     * Similar to saveSymbolKlines but for funding rates. The candleInterval is 480
     * minutes as that's the amount of time between each funding event
     * 
     * @param symbol
     * @param startTime
     * @param endTime
     * @param outFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void saveSymbolFundingRates(String symbol, long startTime, long endTime, File outFile)
            throws IOException, InterruptedException {
        
        // https://fapi.binance.com/fapi/v1/fundingRate?symbol=BTCUSDT&startTime=1554076800000&endTime=1618012800000&limit=1000
        
        saveSymbolData(symbol, Interval._8h, startTime, endTime, outFile, MAX_FUNDING_RATE_REQUEST, FUNDING_RATE_URL);
    }
    
    /**
     * @param symbolFile
     *            key is the symbol and value is its respective file
     * @param candleInterval
     * @param startTime
     * @param endTime
     * @throws InterruptedException
     * @throws IOException
     */
    public void saveMultipleSymbolKlines(Map<String, File> symbolFile, Interval interval, long startTime, long endTime)
            throws InterruptedException, IOException {
        
        for (Map.Entry<String, File> entry : symbolFile.entrySet()) {
            
            saveSymbolKlines(entry.getKey(), interval, startTime, endTime, entry.getValue());
        }
    }
    
    /**
     * Get all klines of candleInterval from the period startTime to endTime and save them
     * to outFile, overwriting it if it already exists
     * 
     * @param symbol
     *            ex: "BTCUSDT"
     * @param candleInterval
     *            1 , 3 , 5 , 15 , 30 minutes only
     * @param startTime
     *            unix timestamp beggining of period
     * @param endTime
     *            unix timestamp end of period
     * @throws InterruptedException
     * @throws IOException
     */
    public void saveSymbolKlines(String symbol, Interval interval, long startTime, long endTime, File outFile)
            throws InterruptedException, IOException {
        
        // https://fapi.binance.com/fapi/v1/klines?symbol=BTCUSDT&candleInterval=1m&startTime=1577836800&endTime=1577836800&limit=1500
        
        saveSymbolData(symbol, interval, startTime, endTime, outFile, MAX_KLINES_REQUEST, KLINES_URL);
    }
    
    public void saveSymbolOrderBook(String symbol, short depth, File outFile) throws IOException, InterruptedException {
        
        if (depth > MAX_ORDERBOOK_REQUEST) {
            depth = MAX_ORDERBOOK_REQUEST;
        }
        
        if (!outFile.exists()) {
            try (FileOutputStream output = new FileOutputStream(outFile)) {
                output.getChannel().write(ByteBuffer.wrap("[".getBytes(StandardCharsets.UTF_8)));
            }
        } else {
            try (FileOutputStream output = new FileOutputStream(outFile, true)) {
                output.getChannel().write(ByteBuffer.wrap(",".getBytes(StandardCharsets.UTF_8)), output.getChannel().size() - 1);
            }
        }
        
        saveSymbolDataSingle(symbol, -1, -1, -1, depth, ORDERBOOK_URL, outFile, true);
        
        try (FileOutputStream output = new FileOutputStream(outFile, true)) {
            output.getChannel().write(ByteBuffer.wrap("]".getBytes(StandardCharsets.UTF_8)));
        }
    }
    
    /**
     * General symbol data method that provides the functionality to implement such
     * methods as saveSymbolKlines and saveSymbolFundingRates
     * 
     * Made to save JSON Arrays to a file. If file already exists it is first deleted.
     * 
     * @param symbol
     * @param candleInterval
     *            candleInterval in minutes between each individual data point, ex: a
     *            kline/candle or funding rate
     * @param startTime
     * @param endTime
     * @param outFile
     * @param maxRequest
     *            maximum number of data points that can be gotten from one GET to
     *            the API
     * @param apiDataUrl
     *            api url fragment corresponding to the required data point
     * @throws IOException
     * @throws InterruptedException
     */
    public void saveSymbolData(String symbol, Interval interval, long startTime, long endTime, File outFile, short maxRequest,
            String apiDataUrl) throws IOException, InterruptedException {
        
        if (outFile.exists()) {
            outFile.delete();
        }
        
        long[] times = getFirstAndLastCandleOpenTime(symbol, interval, startTime, endTime);
        long newTime = times[0];
        endTime = times[1];
        
        long timeToAdd = (long) maxRequest * interval.toMilliseconds();
        
        utilSaveJSONArrayInUrlToFile(null, true, outFile, true);
        
        while (newTime < endTime) {
            
            String getUrl = API_FUTURES_URL + apiDataUrl + "?symbol=" + symbol + "&interval=" + interval + "&startTime=" + newTime
                    + "&endTime=" + endTime + "&limit=" + maxRequest;
            
            utilSaveJSONArrayInUrlToFile(getUrl, false, outFile, true);
            
            newTime += timeToAdd;
        }
        
        utilSaveJSONArrayInUrlToFile(null, false, outFile, true);
    }
    
    public void saveSymbolDataSingle(String symbol, int interval, long startTime, long endTime, short maxRequest,
            String apiDataUrl, File outFile, boolean append) throws IOException, InterruptedException {
        
        try (FileOutputStream output = new FileOutputStream(outFile, append)) {
            
            String getUrl = API_FUTURES_URL + apiDataUrl + "?symbol=" + symbol + "&interval=" + interval + "m&startTime="
                    + startTime + "&endTime=" + endTime + "&limit=" + maxRequest;
            
            output.getChannel().write(ByteBuffer.wrap(utilGetUrlToString(getUrl).getBytes(StandardCharsets.UTF_8)));
        }
    }
    
    /**
     * @param symbol
     * @param interval
     * @param startTime
     * @param endTime
     * @return [0] is first candle, [1] is last candle open time
     * @throws IOException
     * @throws InterruptedException
     */
    public long[] getFirstAndLastCandleOpenTime(String symbol, Interval interval, long startTime, long endTime)
            throws IOException, InterruptedException {
        
        String firstUrl = API_FUTURES_URL + KLINES_URL + "?symbol=" + symbol + "&interval=" + interval.toString()
                + "&startTime=" + startTime + "&limit=" + 1;
        
        String lastUrl = API_FUTURES_URL + KLINES_URL + "?symbol=" + symbol + "&interval=" + interval.toString()
                + "&endTime=" + endTime + "&limit=" + 1;
        
        long[] times = new long[2];
        times[0] = utilGetFirstLongInString(utilGetUrlToString(firstUrl));
        times[1] = utilGetFirstLongInString(utilGetUrlToString(lastUrl));
        
        return times;
    }
    
    public long utilGetFirstLongInString(String string) {
        
        int i = 0;
        while (i < string.length() && !Character.isDigit(string.charAt(i)))
            i++;
        int j = i;
        while (j < string.length() && Character.isDigit(string.charAt(j)))
            j++;
        
        return Long.parseLong(string.substring(i, j));
    }
    
    public String utilGetUrlToString(String url) throws IOException, InterruptedException {
        
        HttpURLConnection connection = utilLimitHttpGET(url);
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = connection.getInputStream().read(buffer)) > 0) {
            bytes.write(buffer, 0, length);
        }
        
        return bytes.toString(StandardCharsets.UTF_8.name());
    }
    
    /**
     * @param url
     *            can be null
     * @param isFirstWrite
     *            if input null and if this param is true will write the beggining
     *            of a new json object, if false as the ending
     * @param out
     * @param doAppend
     * @throws IOException
     * @throws InterruptedException
     */
    public void utilSaveJSONArrayInUrlToFile(String url, boolean isFirstWrite, File out, boolean doAppend)
            throws IOException, InterruptedException {
        
        try (FileOutputStream output = new FileOutputStream(out, doAppend)) {
            
            if (url != null) {
                
                InputStream input = utilLimitHttpGET(url).getInputStream();
                
                byte[] buffer = new byte[1024];
                int length = 0;
                
                if ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 1, length - 1);
                }
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                output.getChannel().write(ByteBuffer.wrap(",".getBytes(StandardCharsets.UTF_8)), output.getChannel().size() - 1);
                
            } else if (isFirstWrite) {
                output.getChannel().write(ByteBuffer.wrap("[".getBytes(StandardCharsets.UTF_8)));
            } else {
                output.getChannel().write(ByteBuffer.wrap("]".getBytes(StandardCharsets.UTF_8)), output.getChannel().size() - 1);
            }
        }
    }
    
    private HttpURLConnection utilLimitHttpGET(String url) throws IOException, InterruptedException {
        
        if (cronometer == -1) {
            cronometer = System.currentTimeMillis();
        }
        
        if (currentWeightValue >= MAX_LIMIT) {
            long elapsed = System.currentTimeMillis() - cronometer;
            if (elapsed <= LIMIT_WINDOW_MILISEC + 500) {
                Thread.sleep(600 + LIMIT_WINDOW_MILISEC - elapsed);
            }
            cronometer = -1;
            currentWeightValue = -1;
            
        } else if (responseCode == STOP_API_SPAM_CODE || responseCode == WAF_RULE_BREAK_API_CODE) {
            Thread.sleep(LIMIT_WINDOW_MILISEC + 400);
            cronometer = -1;
            responseCode = -1;
        }
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        
        connection.setRequestMethod("GET");
        connection.connect();
        
        currentWeightValue = connection.getHeaderFieldInt(WEIGHT_HEADER, 1);
        responseCode = connection.getResponseCode();
        
        System.out.println("weight: " + currentWeightValue + " ; code: " + responseCode);
        
        return connection;
    }
}
