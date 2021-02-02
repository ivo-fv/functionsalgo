package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import functionsalgo.exceptions.ExchangeException;
import functionsalgo.shared.Utils;

//TODO externalize strings to resource bundle and gitignore , make dummy bundle warn to rename before testing

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class APIWrapperIntegrationTest {

    public static WrapperREST bpapi;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Properties keys = Utils.getProperties("binanceperpetual_apikeys_ignore.properties",
                "binanceperpetual_apikeys.properties");
        bpapi = new WrapperREST(keys.getProperty("privateKey"), keys.getProperty("publicApiKey"));
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
        double qty = 3 * exchInfo.getSymbolQtyStepSize("ETHUSDT");
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
        expectedNewAtm = prevAmt - (qty / 3);
        bpapi.marketCloseHedgeMode("ETHUSDT", true, qty / 3);
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
