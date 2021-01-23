package functionalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import mocks.MockLogger;

//TODO externalize strings to resource bundle and gitignore , make dummy bundle warn to rename before testing

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BPAPIWrapperTest {

    private static final String TEST_PRIVATE_KEY = "***REMOVED***";
    private static final String TEST_API_KEY = "***REMOVED***";

    public static MockLogger logger;
    public static BPWrapperREST bpapi;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        logger = new MockLogger();
        bpapi = new BPWrapperREST(logger, TEST_PRIVATE_KEY, TEST_API_KEY, true);
    }

    @Test
    public final void testGetAccountInfo() {
        try {
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

        } catch (Exception e) {
            e.printStackTrace();
            fail(Arrays.toString(e.getStackTrace()));
        }
    }

    @Test
    public final void testGetExchangeInfo() {
        try {
            BPExchangeInfoWrapper exchInfo = bpapi.getExchangeInfo();

            assertTrue("symbolQtyStepSize", exchInfo.getSymbolQtyStepSize("BTCUSDT") >= 0);
            assertTrue("exchangeTime", exchInfo.getExchangeTime() >= 1609459200000L);

        } catch (Exception e) {
            e.printStackTrace();
            fail(Arrays.toString(e.getStackTrace()));
        }
    }

    @Test
    public final void testSetToHedgeMode() {
        try {
            bpapi.setToHedgeMode();
            bpapi.setToHedgeMode();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString() + " || " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Test
    public final void testSetLeverage() {
        try {
            bpapi.setLeverage("BTCUSDT", 20);
            bpapi.setLeverage("BTCUSDT", 1);
            bpapi.setLeverage("BTCUSDT", 20);
            bpapi.setLeverage("BTCUSDT", 20);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString() + " || " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Test
    public final void testSetCrossMargin() {
        try {
            bpapi.setCrossMargin("BTCUSDT");
            bpapi.setCrossMargin("BTCUSDT");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString() + " || " + Arrays.toString(e.getStackTrace()));
        }
    }

    @Test
    public final void testZCloseAllOpenSomeCloseAllAgain() {
        try {
            assertTrue(false);

        } catch (Exception e) {
            e.printStackTrace();
            fail(Arrays.toString(e.getStackTrace()));
        }
    }

}
