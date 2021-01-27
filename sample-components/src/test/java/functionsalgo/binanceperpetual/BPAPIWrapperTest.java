package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import functionsalgo.exceptions.ExchangeException;

//TODO externalize strings to resource bundle and gitignore , make dummy bundle warn to rename before testing

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BPAPIWrapperTest {

    private static final String TEST_PRIVATE_KEY = "b1de68c44b95077fa829d9a904b84c8edc89405ca0ae0f1768cbbdb9cabf841b";
    private static final String TEST_API_KEY = "a02d4409583be65a2721e2de10104e1e6232f402d1fd909cd9390e4aa17aefad";

    public static BPWrapperREST bpapi;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        bpapi = new BPWrapperREST(TEST_PRIVATE_KEY, TEST_API_KEY, true);
    }

    @Test
    public final void testGetAccountInfo() throws ExchangeException {
        BPAccountInfoWrapper accInfo = bpapi.getAccountInfo();

        assertTrue("totalInitialMargin", accInfo.getTotalInitialMargin() >= 0);
        assertTrue("marginBalance", accInfo.getMarginBalance() >= 0);
        assertTrue("walletBalance", accInfo.getWalletBalance() >= 0);

        assertTrue("leverages", accInfo.getLeverages().get("BTCUSDT") >= 1);
        Map.Entry<String, Boolean> entry = accInfo.getIsolatedSymbols().entrySet().iterator().next();
        String isoSymKey = entry.getKey();
        assertTrue("isolatedSymbols", isoSymKey.length() >= 2);

        Map<String, Double> longPositions = accInfo.getLongPositions();

        if (longPositions.size() > 0) {
            assertTrue("longPositions", longPositions.get("BTCUSDT") >= 0);
            assertTrue("shortPositions", accInfo.getShortPositions().get("BTCUSDT") >= 0);
        } else {
            assertTrue("bothPositions", accInfo.getBothPositions().get("BTCUSDT") >= 0);
        }
    }

    @Test
    public final void testGetExchangeInfo() throws ExchangeException {
        BPExchangeInfoWrapper exchInfo = bpapi.getExchangeInfo();

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

        BPAccountInfoWrapper accInfo = bpapi.getAccountInfo();
        assertFalse("crossMargin isolated", accInfo.getIsolatedSymbols().get("BTCUSDT"));
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
        BPExchangeInfoWrapper exchInfo = bpapi.getExchangeInfo();
        double qty = 3 * exchInfo.getSymbolQtyStepSize("ETHUSDT");
        BPAccountInfoWrapper accInfo = bpapi.getAccountInfo();
        double prevAmt = accInfo.getLongPositions().get("ETHUSDT");
        // open
        double expectedNewAtm = prevAmt + qty;
        bpapi.marketOpenHedgeMode("ETHUSDT", true, qty);
        // check
        accInfo = bpapi.getAccountInfo();
        double newAmt = accInfo.getLongPositions().get("ETHUSDT");
        assertTrue("expected amount 1", expectedNewAtm == newAmt);
        prevAmt = newAmt;
        // close some
        expectedNewAtm = prevAmt - (qty / 3);
        bpapi.marketCloseHedgeMode("ETHUSDT", true, qty / 3);
        // check
        accInfo = bpapi.getAccountInfo();
        newAmt = accInfo.getLongPositions().get("ETHUSDT");
        assertTrue("expected amount 2", expectedNewAtm == newAmt);
        // close rest
        bpapi.marketCloseHedgeMode("ETHUSDT", true, 999);
        // check
        accInfo = bpapi.getAccountInfo();
        newAmt = accInfo.getLongPositions().get("ETHUSDT");
        assertTrue("expected amount 3", newAmt == 0);
    }
}
