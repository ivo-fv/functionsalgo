package functionsalgo.binanceperpetual.exchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import functionsalgo.binanceperpetual.AccountInfoWrapper;
import functionsalgo.binanceperpetual.ExchangeInfoWrapper;
import functionsalgo.binanceperpetual.PositionWrapper;
import functionsalgo.binanceperpetual.WrapperREST;
import functionsalgo.binanceperpetual.WrapperRESTException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolNotTradingException;
import functionsalgo.binanceperpetual.exchange.exceptions.SymbolQuantityTooLow;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LiveExchangeTest {

    static WrapperREST api;
    static LiveExchange exchange;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        api = Mockito.mock(WrapperREST.class);
        exchange = new LiveExchange("test", "test");
        exchange.api = api;

        HashMap<String, Boolean> symbolTrading = new HashMap<>();
        symbolTrading.put("BTCUSDT", true);
        symbolTrading.put("ETHUSDT", true);
        symbolTrading.put("SYMBOL", false);
        HashMap<String, Double> symbolQtyStepSize = new HashMap<>();
        symbolQtyStepSize.put("BTCUSDT", 0.001);
        symbolQtyStepSize.put("ETHUSDT", 0.001);
        ExchangeInfoWrapper stubExchInfo = new ExchangeInfoWrapper(symbolTrading, symbolQtyStepSize, 1611876740899L);
        doReturn(stubExchInfo).when(api).getExchangeInfo();
    }

    @Test
    public final void testGetAccountInfo() throws WrapperRESTException {

        HashMap<String, Integer> leverages = new HashMap<>();
        leverages.put("BTCUSDT", 10);
        HashMap<String, PositionWrapper> longs = new HashMap<>();
        longs.put("BTCUSDT", new PositionWrapper("BTCUSDT", 12, 21, false, false, true));
        HashMap<String, PositionWrapper> shorts = new HashMap<>();
        HashMap<String, PositionWrapper> both = new HashMap<>();
        AccountInfoWrapper stubAccInfo = new AccountInfoWrapper(1000, 1200, 1300, leverages, longs, shorts, both, true);
        doReturn(stubAccInfo).when(api).getAccountInfo();

        AccountInfo accInfo = exchange.getAccountInfo(0);

        verify(api, times(1)).getExchangeInfo();
        verify(api, times(1)).getAccountInfo();

        assertTrue("getTimestamp", accInfo.getTimestamp() == 1611876740899L);
        assertTrue("getTotalInitialMargin", accInfo.getTotalInitialMargin() == 1000);
        assertTrue("getMarginBalance", accInfo.getMarginBalance() == 1200);
        assertTrue("getWalletBalance", accInfo.getWalletBalance() == 1300);
        assertTrue("getLeverage", accInfo.getLeverage("BTCUSDT") == 10);
        assertTrue("getQuantity", accInfo.getQuantity("BTCUSDT", true) == 12);
        assertTrue("getAverageOpenPrice", accInfo.getAverageOpenPrice("BTCUSDT", true) == 21);
        assertTrue("isSymbolIsolated", accInfo.isSymbolIsolated("BTCUSDT", true) == false);
        assertTrue("isHedgeMode", accInfo.isHedgeMode() == true);

        try {
            accInfo.getQuantity("BTCUSDT", false);
            fail("getQuantity short");
        } catch (Exception e) {
            // must catch the exception to pass the test
        }
    }

    @Test
    public final void testSetHedgeMode() throws WrapperRESTException {
        exchange.setHedgeMode();
        verify(api, times(1)).setToHedgeMode();
    }

    @Test
    public final void setLeverage() throws WrapperRESTException {
        exchange.setLeverage("ETHUSDT", 10);
        verify(api, times(1)).setLeverage("ETHUSDT", 10);
    }

    @Test
    public final void setCrossMargin() throws WrapperRESTException {
        exchange.setCrossMargin("ETHUSDT");
        verify(api, times(1)).setToCrossMargin("ETHUSDT");
    }

    @Test
    public final void testZ0ExceptionsAddBatchMarketOpen() {
        try {
            exchange.addBatchMarketOpen("123", "SYMBOL", true, 1);
            fail("addBatchMarketOpen SYMBOL isn't trading, must throw SymbolNotTradingException");
        } catch (Exception e) {
            assertTrue("exception must be SymbolNotTradingException", e instanceof SymbolNotTradingException);
        }
        try {
            exchange.addBatchMarketOpen("123", "NOSYMBOL", true, 1);
            fail("addBatchMarketOpen NOSYMBOL doesn't exist, must throw NullPointerException");
        } catch (Exception e) {
            assertTrue("exception must be NullPointerException", e instanceof NullPointerException);
        }
        try {
            exchange.addBatchMarketOpen("123", "BTCUSDT", false, 0.00001);
            fail("addBatchMarketOpen BTCUSDT too low quantity, must throw SymbolQuantityTooLow (can fail the next tests)");
        } catch (Exception e) {
            assertTrue("exception must be SymbolQuantityTooLow", e instanceof SymbolQuantityTooLow);
        }
    }

    @Test
    public final void testZ1ExceptionsAddBatchMarketClose() {
        try {
            exchange.addBatchMarketClose("123", "SYMBOL", true, 1);
            fail("addBatchMarketOpen SYMBOL isn't trading, must throw SymbolNotTradingException");
        } catch (Exception e) {
            assertTrue("exception must be SymbolNotTradingException", e instanceof SymbolNotTradingException);
        }
        try {
            exchange.addBatchMarketClose("123", "NOSYMBOL", true, 1);
            fail("addBatchMarketOpen NOSYMBOL doesn't exist, must throw NullPointerException");
        } catch (Exception e) {
            assertTrue("exception must be NullPointerException", e instanceof NullPointerException);
        }
        try {
            exchange.addBatchMarketClose("123", "BTCUSDT", false, 0.00001);
            fail("addBatchMarketOpen BTCUSDT too low quantity, must throw SymbolQuantityTooLow (can fail the next tests)");
        } catch (Exception e) {
            assertTrue("exception must be SymbolQuantityTooLow", e instanceof SymbolQuantityTooLow);
        }
    }

    @Test
    public final void testZ2AddBatchMarketOpenAndExecuteBatchedMarketOpenOrders() {

        // addbatch -> stub markeOpenHedgeMode -> execute -> verify wrapperapi with
        // expected params -> assert AccountInfo consistent (must stub getinfo too
        // probably, or do it once setupbeforeclass)

        fail("not implemented yet");
    }

    @Test
    public final void testZ3AddBatchMarketCloseAndExecuteBatchedMarketCloseOrders() {
        fail("not implemented yet");
    }

    @Test
    public final void testZ4ErrorsAddBatchMarketOpenAndExecuteBatchedMarketOpenOrders() {

        // addbatch -> stub markeOpenHedgeMode -> execute -> verify wrapperapi with
        // expected params -> assert AccountInfo has expected errors
        // errors when code is returned must be OrderError.FAILED else
        // OrderError.UNKNOWN, so 2 stubs one must throw exception with code, the other
        // with errortype

        fail("not implemented yet");
    }

    @Test
    public final void testZ5ErrorsAddBatchMarketCloseAndExecuteBatchedMarketCloseOrders() {
        fail("not implemented yet");
    }

}
