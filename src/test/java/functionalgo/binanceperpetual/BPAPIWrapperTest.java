package functionalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import functionalgo.exceptions.ExchangeException;
import mocks.MockLogger;

public class BPAPIWrapperTest {

    private static final String TEST_PRIVATE_KEY = "***REMOVED***";
    private static final String TEST_API_KEY = "***REMOVED***";

    public static MockLogger logger;
    public static BPAPIWrapper bpapi;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        logger = new MockLogger();
        bpapi = new BPAPIWrapper(logger, TEST_PRIVATE_KEY, TEST_API_KEY, true);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testGetAccInfTotalInitialMargin() {
        double totalInitialMargin = -999;
        try {
            totalInitialMargin = bpapi.getAccInfTotalInitialMargin(false);
            assertTrue(">=0", totalInitialMargin >= 0);

        } catch (ExchangeException e) {
            e.printStackTrace();
            fail("structure incorrect");
        }
    }

    @Test
    public final void testGetAccInfMarginBalance() {

        double marginBalance = -999;
        try {
            marginBalance = bpapi.getAccInfMarginBalance(false);
            assertTrue(">=0", marginBalance >= 0);

        } catch (ExchangeException e) {
            e.printStackTrace();
            fail("structure incorrect");
        }
    }

    @Test
    public final void testGetAccInfWalletBalance() {
        double walletBalance = -999;
        try {
            walletBalance = bpapi.getAccInfWalletBalance(false);
            assertTrue(">=0", walletBalance >= 0);

        } catch (ExchangeException e) {
            e.printStackTrace();
            fail("structure incorrect");
        }
    }

    @Test
    public final void testGetAccInfLeverages() {

        Map<String, Integer> leverages = null;

        try {
            leverages = bpapi.getAccInfLeverages(false);

            Map.Entry<String, Integer> entry = leverages.entrySet().iterator().next();
            String key = entry.getKey();
            int value = entry.getValue();

            assertTrue("firt symbol must have leverage >= 1", value >= 1);

        } catch (ExchangeException e) {
            e.printStackTrace();
            fail("structure incorrect");
        }
    }

    @Test
    public final void testGetAccInfIsolatedSymbols() {

        Map<String, Boolean> isolatedSymbols = null;

        try {
            isolatedSymbols = bpapi.getAccInfIsolatedSymbols(false);

            Map.Entry<String, Boolean> entry = isolatedSymbols.entrySet().iterator().next();
            String key = entry.getKey();

            assertNotNull("map must exist", isolatedSymbols);
            assertTrue("key must exist", key.length() >= 2);

        } catch (ExchangeException e) {
            e.printStackTrace();
            fail("structure incorrect");
        }
    }

    @Test
    public final void testIsAccInfHedgeMode() {

        Object isHedgeMode = null;

        try {
            isHedgeMode = bpapi.isAccInfHedgeMode(false);

            assertTrue("must be a boolean value", isHedgeMode instanceof Boolean);

        } catch (ExchangeException e) {
            e.printStackTrace();
            fail("structure incorrect");
        }
    }

}
