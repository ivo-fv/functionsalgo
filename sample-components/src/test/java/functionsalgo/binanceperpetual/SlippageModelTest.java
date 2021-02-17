package functionsalgo.binanceperpetual;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import functionsalgo.exceptions.StandardJavaException;

public class SlippageModelTest {

    @Test
    public final void testSlippageModelAll() throws StandardJavaException {

        SlippageModel.DATA_DIR = "../.genresources/test_data";
        SlippageModel.MODEL_FILE = SlippageModel.DATA_DIR + "/slippage_model";
        SlippageModel.JSON_ORDER_BOOKS_FOLDER = SlippageModel.DATA_DIR + "/binance_perp_json_order_books";

        SlippageModel.MAX_RANGE_VALUE = 20000;
        SlippageModel.SLIPPAGE_STEPS = 100;
        SlippageModel.BOOK_DEPTH = 20;

        ArrayList<String> symbols = new ArrayList<>();
        symbols.add("BTCUSDT");
        symbols.add("ETHUSDT");

        SlippageModel slippageModel = SlippageModel.pullSlippageModel(symbols);

        assertTrue("invalid SlippageModel object file - getSlippage", slippageModel.getSlippage(1322, "BTCUSDT") >= 1);
        assertTrue("invalid SlippageModel object file - getSlippage", slippageModel.getSlippage(1322, "ETHUSDT") >= 1);
    }

}
