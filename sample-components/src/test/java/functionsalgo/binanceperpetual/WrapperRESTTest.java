package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import functionsalgo.datapoints.Interval;
import functionsalgo.exceptions.ExchangeException;
import functionsalgo.exceptions.StandardJavaException;
import functionsalgo.shared.Utils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WrapperRESTTest {

    private static final Logger logger = LogManager.getLogger();

    public static final File encryptedSecrets = new File("src/test/resources/encrypted_secrets");

    public static WrapperREST bpapi;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Properties keys = new Properties();
        try {
            keys.load(Utils.getFileOrResource("secrets_ignore.properties").openStream());
            URL keyUrl = Utils.getFileOrResource("../secrets_key_ignore", "secrets_key_ignore");
            Utils.saveAndEncryptProperties(keys, encryptedSecrets.toURI().toURL(), keyUrl);
        } catch (IOException | StandardJavaException e) {
            logger.warn(
                    "secrets_ignore or secrets_key_ignore was likely not found, trying with encrypted_secrets and env key FUNCTIONSALGO_SECRETS_KEY",
                    e);
            URL secretsUrl = Utils.getFileOrResource("encrypted_secrets", "src/test/resources/encrypted_secrets");
            keys = Utils.loadEncryptedProperties(secretsUrl, System.getenv("FUNCTIONSALGO_SECRETS_KEY"));
        }

        // "secrets.properties" by default doesn't have valid keys, so
        // must modify the file with proper secrets
        if (keys.getProperty("binanceperpetual.publicApiKey").length() < 60) {
            keys.load(Utils.getFileOrResource("secrets.properties").openStream());
        }
        bpapi = new WrapperREST(keys.getProperty("binanceperpetual.privateKey"),
                keys.getProperty("binanceperpetual.publicApiKey"));
        bpapi.setToTestHost();
    }

    @Test
    public final void testGetAccountInfo() throws ExchangeException {
        AccountInfoWrapper accInfo = bpapi.getAccountInfo();

        assertTrue("totalInitialMargin", accInfo.getTotalInitialMargin() >= 0);
        assertTrue("marginBalance", accInfo.getMarginBalance() >= 0);
        assertTrue("walletBalance", accInfo.getWalletBalance() >= 0);

        assertTrue("leverages", accInfo.getLeverages().get("BTCUSDT") >= 1);

        if (accInfo.getLongPositions().size() > 0) {
            assertTrue("averagePrice", accInfo.getLongPositions().get("BTCUSDT").averagePrice >= 0);
            assertFalse("isBoth", accInfo.getLongPositions().get("BTCUSDT").isBoth);
            assertTrue("isLong", accInfo.getLongPositions().get("BTCUSDT").isLong);
            assertTrue("quantity", accInfo.getLongPositions().get("BTCUSDT").quantity >= 0);
            assertTrue("symbol", accInfo.getLongPositions().get("BTCUSDT").symbol.equals("BTCUSDT"));
        }

    }

    @Test
    public final void testGetExchangeInfo() throws ExchangeException {
        ExchangeInfoWrapper exchInfo = bpapi.getExchangeInfo();

        assertTrue("symbolQtyStepSize", exchInfo.getSymbolQtyStepSize("BTCUSDT") >= 0);
        assertTrue("exchangeTime", exchInfo.getExchangeTime() >= 1609459200000L);
    }

    @Test
    public final void testSetToHedgeMode() throws ExchangeException {
        bpapi.setToHedgeMode();
        bpapi.setToHedgeMode();
    }

    @Test
    public final void testSetLeverage() throws ExchangeException {
        bpapi.setLeverage("BTCUSDT", 20);
        bpapi.setLeverage("BTCUSDT", 20);
    }

    @Test
    public final void testSetToCrossMargin() throws ExchangeException {
        bpapi.setToCrossMargin("BTCUSDT");
        bpapi.setToCrossMargin("BTCUSDT");
    }

    @Test
    public final void testSetLeverageSetToHedgeModeSetToCrossMarginAndCheck() throws ExchangeException {
        // test set leverage,cross,hedge and check
        bpapi.setToCrossMargin("BTCUSDT");
        bpapi.setLeverage("BTCUSDT", 5);
        bpapi.setToHedgeMode();

        AccountInfoWrapper accInfo = bpapi.getAccountInfo();
        assertFalse("crossMargin isolated", accInfo.getShortPositions().get("BTCUSDT").isIsolated);
        assertTrue("leverage 1", accInfo.getLeverages().get("BTCUSDT") == 5);
        assertTrue("hedgeMode", accInfo.isHedgeMode());

        bpapi.setLeverage("BTCUSDT", 20);

        accInfo = bpapi.getAccountInfo();
        assertTrue("leverage 2", accInfo.getLeverages().get("BTCUSDT") == 20);
    }

    @Test
    public final void testMarketOpenHedgeModeAndMarketCloseHedgeMode() throws ExchangeException {
        String res = bpapi.marketOpenHedgeMode("BTCUSDT", true, 0.02).getSymbol();
        assertTrue("open symbol", res.equals("BTCUSDT"));

        res = bpapi.marketCloseHedgeMode("BTCUSDT", true, 0.02).getSymbol();
        assertTrue("close symbol", res.equals("BTCUSDT"));
    }

    @Test
    public final void testSaveKlinesMult() throws IOException {
        File btcFile = new File("BTCUSDT.json");
        for (int i = 0; i < 10 && btcFile.exists(); i++) {
            btcFile.delete();
        }
        File ethFile = new File("ETHUSDT.json");
        for (int i = 0; i < 10 && ethFile.exists(); i++) {
            ethFile.delete();
        }

        ArrayList<File> klinesFiles = new ArrayList<>();
        klinesFiles.add(new File("BTCUSDT.json"));
        klinesFiles.add(new File("ETHUSDT.json"));

        ArrayList<String> symbols = new ArrayList<>();
        symbols.add("BTCUSDT");
        symbols.add("ETHUSDT");

        bpapi.saveKlines(klinesFiles, symbols, Interval._5m, 1611646639946L, 1612470647356L);

        assertTrue("file wasn't created", new File("BTCUSDT.json").exists() && new File("ETHUSDT.json").exists());

        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("BTCUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement btc = JsonParser.parseReader(is);
            assertTrue("must be array", btc.isJsonArray());
        }
        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("ETHUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement eth = JsonParser.parseReader(is);
            assertTrue("must be array", eth.isJsonArray());
        }

        for (int i = 0; i < 10 && btcFile.exists(); i++) {
            btcFile.delete();
        }
        for (int i = 0; i < 10 && ethFile.exists(); i++) {
            ethFile.delete();
        }
    }

    @Test
    public final void testSaveKlinesSingle() throws IOException {
        File ethFile = new File("ETHUSDT.json");
        for (int i = 0; i < 10 && ethFile.exists(); i++) {
            ethFile.delete();
        }
        String ethSymbol = "ETHUSDT";

        bpapi.saveKlines(ethFile, ethSymbol, Interval._5m, 1612400647356L, 1612470647356L);

        assertTrue("file wasn't created", ethFile.exists());

        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("ETHUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement eth = JsonParser.parseReader(is);
            assertTrue("must be array", eth.isJsonArray());
        }

        for (int i = 0; i < 10 && ethFile.exists(); i++) {
            ethFile.delete();
        }
    }

    @Test
    public final void testSaveFundingRatesMult() throws IOException {
        File btcFile = new File("BTCUSDT.json");
        Utils.deleteFile(btcFile);
        File ethFile = new File("ETHUSDT.json");
        Utils.deleteFile(ethFile);

        ArrayList<File> fratesFiles = new ArrayList<>();
        fratesFiles.add(new File("BTCUSDT.json"));
        fratesFiles.add(new File("ETHUSDT.json"));

        ArrayList<String> symbols = new ArrayList<>();
        symbols.add("BTCUSDT");
        symbols.add("ETHUSDT");

        bpapi.saveFundingRates(fratesFiles, symbols, 1591646639946L, 1612470647356L);

        assertTrue("file wasn't created", new File("BTCUSDT.json").exists() && new File("ETHUSDT.json").exists());

        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("BTCUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement btc = JsonParser.parseReader(is);
            assertTrue("must be array", btc.isJsonArray());
        }
        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("ETHUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement eth = JsonParser.parseReader(is);
            assertTrue("must be array", eth.isJsonArray());
        }
        Utils.deleteFile(btcFile);
        Utils.deleteFile(ethFile);
    }

    @Test
    public final void testSaveFundingRatesSingle() throws IOException {
        File ethFile = new File("ETHUSDT.json");
        Utils.deleteFile(ethFile);
        String ethSymbol = "ETHUSDT";

        bpapi.saveFundingRates(ethFile, ethSymbol, 1591646639946L, 1612470647356L);

        assertTrue("file wasn't created", ethFile.exists());

        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("ETHUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement eth = JsonParser.parseReader(is);
            assertTrue("must be array", eth.isJsonArray());
        }
        Utils.deleteFile(ethFile);
    }

    @Test
    public final void testSaveOrderBooks() throws IOException {
        File btcFile = new File("BTCUSDT.json");
        Utils.deleteFile(btcFile);
        File ethFile = new File("ETHUSDT.json");
        Utils.deleteFile(ethFile);

        ArrayList<File> orderBookFiles = new ArrayList<>();
        orderBookFiles.add(new File("BTCUSDT.json"));
        orderBookFiles.add(new File("ETHUSDT.json"));

        ArrayList<String> symbols = new ArrayList<>();
        symbols.add("BTCUSDT");
        symbols.add("ETHUSDT");

        bpapi.saveOrderBooks(orderBookFiles, symbols, 50, true);

        assertTrue("file wasn't created", new File("BTCUSDT.json").exists() && new File("ETHUSDT.json").exists());

        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("BTCUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement btc = JsonParser.parseReader(is);
            assertTrue("must be array", btc.isJsonArray());
        }
        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("ETHUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonElement eth = JsonParser.parseReader(is);
            assertTrue("must be array", eth.isJsonArray());
        }

        bpapi.saveOrderBooks(orderBookFiles, symbols, 50, true);

        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("BTCUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonArray btc = JsonParser.parseReader(is).getAsJsonArray();
            assertTrue("size must be 2", btc.size() == 2);
        }
        try (InputStreamReader is = new InputStreamReader(new FileInputStream(new File("ETHUSDT.json")),
                StandardCharsets.UTF_8)) {
            JsonArray eth = JsonParser.parseReader(is).getAsJsonArray();
            assertTrue("size must be 2", eth.size() == 2);
        }

        Utils.deleteFile(btcFile);
        Utils.deleteFile(ethFile);
    }

    @Test
    public final void testZ1CloseAllOpenSomeCloseAllAgain() throws ExchangeException {
        // open a little close all of it with large qty
        bpapi.marketOpenHedgeMode("BTCUSDT", true, 0.002);
        bpapi.marketCloseHedgeMode("BTCUSDT", true, 999);
        bpapi.marketOpenHedgeMode("BTCUSDT", false, 0.002);
        bpapi.marketCloseHedgeMode("BTCUSDT", false, 999);

        // close non existent
        try {
            bpapi.marketCloseHedgeMode("BTCUSDT", false, 999);
            fail("must throw");
        } catch (ExchangeException e) {
            assertTrue("close code", e.getCode() == -2022);
        }
    }

    @Test
    public final void testZ2OpenSomeCheckValuesCloseCheckAgain() throws ExchangeException {
        // check, open, check, close some, check, close rest, check
        // check
        ExchangeInfoWrapper exchInfo = bpapi.getExchangeInfo();
        double qty = 10 * exchInfo.getSymbolQtyStepSize("ETHUSDT");
        AccountInfoWrapper accInfo = bpapi.getAccountInfo();
        double prevAmt = accInfo.getLongPositions().get("ETHUSDT").quantity;
        // open
        double expectedNewAtm = prevAmt + qty;
        bpapi.marketOpenHedgeMode("ETHUSDT", true, qty);
        // check
        accInfo = bpapi.getAccountInfo();
        double newAmt = accInfo.getLongPositions().get("ETHUSDT").quantity;
        assertTrue("expected amount 1", expectedNewAtm == newAmt);
        prevAmt = newAmt;
        // close some
        expectedNewAtm = prevAmt - (qty / 2);
        double trimQty = Utils.trimDec(qty / 2, BigDecimal.valueOf(exchInfo.getSymbolQtyStepSize("ETHUSDT")));
        bpapi.marketCloseHedgeMode("ETHUSDT", true, trimQty);
        // check
        accInfo = bpapi.getAccountInfo();
        newAmt = accInfo.getLongPositions().get("ETHUSDT").quantity;
        assertTrue("expected amount 2", expectedNewAtm == newAmt);
        // close rest
        bpapi.marketCloseHedgeMode("ETHUSDT", true, 999);
        // check
        accInfo = bpapi.getAccountInfo();
        newAmt = accInfo.getLongPositions().get("ETHUSDT").quantity;
        assertTrue("expected amount 3", newAmt == 0);
    }
}
