package functionsalgo.binanceperpetual.exchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import functionsalgo.binanceperpetual.AccountInfoWrapper;
import functionsalgo.binanceperpetual.ExchangeInfoWrapper;
import functionsalgo.binanceperpetual.OrderResultWrapper;
import functionsalgo.binanceperpetual.PositionWrapper;
import functionsalgo.binanceperpetual.WrapperREST;
import functionsalgo.binanceperpetual.WrapperRESTException;
import functionsalgo.binanceperpetual.exchange.exceptions.OrderExecutionException;
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
        // getExchangeInfo stub
        HashMap<String, Boolean> symbolTrading = new HashMap<>();
        symbolTrading.put("BTCUSDT", true);
        symbolTrading.put("ETHUSDT", true);
        symbolTrading.put("SYMBOL", false);
        HashMap<String, Double> symbolQtyStepSize = new HashMap<>();
        symbolQtyStepSize.put("BTCUSDT", 0.001);
        symbolQtyStepSize.put("ETHUSDT", 0.001);
        ExchangeInfoWrapper stubExchInfo = new ExchangeInfoWrapper(symbolTrading, symbolQtyStepSize, 1611876740899L);
        doReturn(stubExchInfo).when(api).getExchangeInfo();
        // getAccountInfo stub
        HashMap<String, Integer> leverages = new HashMap<>();
        leverages.put("BTCUSDT", 10);
        HashMap<String, PositionWrapper> longs = new HashMap<>();
        longs.put("BTCUSDT", new PositionWrapper("BTCUSDT", 12, 21, false, false, true));
        HashMap<String, PositionWrapper> shorts = new HashMap<>();
        HashMap<String, PositionWrapper> both = new HashMap<>();
        AccountInfoWrapper stubAccInfo = new AccountInfoWrapper(1000, 1200, 1300, leverages, longs, shorts, both, true);
        doReturn(stubAccInfo).when(api).getAccountInfo();
    }

    @Test
    public final void testGetAccountInfo() throws WrapperRESTException {

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
    public final void testZ2AddBatchMarketOpenAndExecuteBatchedMarketOpenOrders()
            throws SymbolQuantityTooLow, SymbolNotTradingException, OrderExecutionException, WrapperRESTException {

        exchange.addBatchMarketOpen("1234", "BTCUSDT", false, 1);
        exchange.addBatchMarketOpen("1235", "ETHUSDT", true, 1.2);
        exchange.addBatchMarketOpen("1235", "ETHUSDT", true, 0.03);

        doAnswer(i -> {
            String symbol = i.getArgument(0);
            return new OrderResultWrapper(symbol);
        }).when(api).marketOpenHedgeMode(any(String.class), any(Boolean.class), any(Double.class));

        AccountInfo retAccInfo = exchange.executeBatchedMarketOpenOrders();

        verify(api, times(1)).marketOpenHedgeMode("BTCUSDT", false, 1);
        verify(api, times(1)).marketOpenHedgeMode("ETHUSDT", true, 1.2);
        verify(api, times(1)).marketOpenHedgeMode("ETHUSDT", true, 0.03);
        verify(api, times(3)).marketOpenHedgeMode(any(String.class), any(Boolean.class), any(Double.class));

        assertTrue("errors must be empty", retAccInfo.getOrderErrors().isEmpty());
    }

    @Test
    public final void testZ3AddBatchMarketCloseAndExecuteBatchedMarketCloseOrders()
            throws SymbolQuantityTooLow, SymbolNotTradingException, WrapperRESTException, OrderExecutionException {

        exchange.addBatchMarketClose("1234", "BTCUSDT", false, 1);
        exchange.addBatchMarketClose("1235", "ETHUSDT", true, 1.2);
        exchange.addBatchMarketClose("1235", "ETHUSDT", true, 0.03);

        doAnswer(i -> {
            String symbol = i.getArgument(0);
            return new OrderResultWrapper(symbol);
        }).when(api).marketCloseHedgeMode(any(String.class), any(Boolean.class), any(Double.class));

        AccountInfo retAccInfo = exchange.executeBatchedMarketCloseOrders();

        verify(api, times(1)).marketCloseHedgeMode("BTCUSDT", false, 1);
        verify(api, times(1)).marketCloseHedgeMode("ETHUSDT", true, 1.2);
        verify(api, times(1)).marketCloseHedgeMode("ETHUSDT", true, 0.03);
        verify(api, times(3)).marketCloseHedgeMode(any(String.class), any(Boolean.class), any(Double.class));

        assertTrue("errors must be empty", retAccInfo.getOrderErrors().isEmpty());
    }

    @Test
    public final void testZ4ErrorsAddBatchMarketOpenAndExecuteBatchedMarketOpenOrders()
            throws SymbolQuantityTooLow, SymbolNotTradingException, WrapperRESTException, OrderExecutionException {

        exchange.addBatchMarketOpen("1234", "BTCUSDT", false, 1.1);
        exchange.addBatchMarketOpen("1235", "ETHUSDT", true, 1.3);
        exchange.addBatchMarketOpen("1236", "ETHUSDT", true, 0.04);
        exchange.addBatchMarketOpen("1237", "ETHUSDT", true, 2);

        // triggers INCONSISTENT_ORDER_RESULT check - OrderStatus.UNKNOWN
        doReturn(new OrderResultWrapper("XXXUSDT")).when(api).marketOpenHedgeMode(eq("BTCUSDT"), any(Boolean.class),
                any(Double.class));
        // triggers code check - OrderStatus.FAILED
        doThrow(new WrapperRESTException(2, "some error code", "WrapperREST::marketOpenHedgeMode")).when(api)
                .marketOpenHedgeMode("ETHUSDT", true, 1.3);
        // triggers non-code check - OrderStatus.UNKNOWN
        doThrow(new WrapperRESTException(WrapperRESTException.ErrorType.UNKNOWN_RESPONSE, "some error code",
                "WrapperREST::marketOpenHedgeMode")).when(api).marketOpenHedgeMode("ETHUSDT", true, 0.04);
        // expect no errors
        doAnswer(i -> {
            String symbol = i.getArgument(0);
            return new OrderResultWrapper(symbol);
        }).when(api).marketOpenHedgeMode("ETHUSDT", true, 2);

        AccountInfo retAccInfo = exchange.executeBatchedMarketOpenOrders();

        verify(api, times(1)).marketOpenHedgeMode("BTCUSDT", false, 1.1);
        verify(api, times(1)).marketOpenHedgeMode("ETHUSDT", true, 1.3);
        verify(api, times(1)).marketOpenHedgeMode("ETHUSDT", true, 0.04);
        verify(api, times(1)).marketOpenHedgeMode("ETHUSDT", true, 2);

        assertTrue("there must be 3 errors", retAccInfo.getOrderErrors().size() == 3);

        for (OrderError error : retAccInfo.getOrderErrors()) {
            switch (error.getOrderId()) {
            case "1234":
                assertTrue("id 1234 must have OrderStatus.UNKNOWN",
                        error.getStatus() == OrderError.OrderStatus.UNKNOWN);
                break;
            case "1235":
                assertTrue("id 1235 must have OrderStatus.FAILED", error.getStatus() == OrderError.OrderStatus.FAILED);
                break;
            case "1236":
                assertTrue("id 1236 must have OrderStatus.UNKNOWN",
                        error.getStatus() == OrderError.OrderStatus.UNKNOWN);
                break;
            case "1237":
                fail("id 1237 must not be in an error");
                break;
            default:
                fail("no expected orderId");
            }
        }
    }

    @Test
    public final void testZ5ErrorsAddBatchMarketCloseAndExecuteBatchedMarketCloseOrders()
            throws SymbolQuantityTooLow, SymbolNotTradingException, WrapperRESTException, OrderExecutionException {
        exchange.addBatchMarketClose("1234", "BTCUSDT", false, 1.1);
        exchange.addBatchMarketClose("1235", "ETHUSDT", true, 1.3);
        exchange.addBatchMarketClose("1236", "ETHUSDT", true, 0.04);
        exchange.addBatchMarketClose("1237", "ETHUSDT", true, 2);

        // triggers INCONSISTENT_ORDER_RESULT check - OrderStatus.UNKNOWN
        doReturn(new OrderResultWrapper("XXXUSDT")).when(api).marketCloseHedgeMode(eq("BTCUSDT"), any(Boolean.class),
                any(Double.class));
        // triggers code check - OrderStatus.FAILED
        doThrow(new WrapperRESTException(2, "some error code", "WrapperREST::marketOpenHedgeMode")).when(api)
                .marketCloseHedgeMode("ETHUSDT", true, 1.3);
        // triggers non-code check - OrderStatus.UNKNOWN
        doThrow(new WrapperRESTException(WrapperRESTException.ErrorType.UNKNOWN_RESPONSE, "some error code",
                "WrapperREST::marketOpenHedgeMode")).when(api).marketCloseHedgeMode("ETHUSDT", true, 0.04);
        // expect no errors
        doAnswer(i -> {
            String symbol = i.getArgument(0);
            return new OrderResultWrapper(symbol);
        }).when(api).marketCloseHedgeMode("ETHUSDT", true, 2);

        AccountInfo retAccInfo = exchange.executeBatchedMarketCloseOrders();

        verify(api, times(1)).marketCloseHedgeMode("BTCUSDT", false, 1.1);
        verify(api, times(1)).marketCloseHedgeMode("ETHUSDT", true, 1.3);
        verify(api, times(1)).marketCloseHedgeMode("ETHUSDT", true, 0.04);
        verify(api, times(1)).marketCloseHedgeMode("ETHUSDT", true, 2);

        assertTrue("there must be 3 errors", retAccInfo.getOrderErrors().size() == 3);

        for (OrderError error : retAccInfo.getOrderErrors()) {
            switch (error.getOrderId()) {
            case "1234":
                assertTrue("id 1234 must have OrderStatus.UNKNOWN",
                        error.getStatus() == OrderError.OrderStatus.UNKNOWN);
                break;
            case "1235":
                assertTrue("id 1235 must have OrderStatus.FAILED", error.getStatus() == OrderError.OrderStatus.FAILED);
                break;
            case "1236":
                assertTrue("id 1236 must have OrderStatus.UNKNOWN",
                        error.getStatus() == OrderError.OrderStatus.UNKNOWN);
                break;
            case "1237":
                fail("id 1237 must not be in an error");
                break;
            default:
                fail("no expected orderId");
            }
        }
    }

}
