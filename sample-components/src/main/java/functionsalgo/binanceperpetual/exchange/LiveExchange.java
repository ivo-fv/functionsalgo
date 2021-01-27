package functionsalgo.binanceperpetual.exchange;

import functionsalgo.binanceperpetual.AccountInfoWrapper;
import functionsalgo.binanceperpetual.ExchangeInfoWrapper;
import functionsalgo.binanceperpetual.WrapperREST;
import functionsalgo.exceptions.ExchangeException;

public class LiveExchange implements Exchange {

    WrapperREST api;

    private double totalInitialMargin = -1;
    private ExchangeInfoWrapper exchangeInfo;

    public LiveExchange(String privateKey, String apiKey) throws ExchangeException {
        try {
            api = new WrapperREST(privateKey, apiKey);
        } catch (Exception e) {
            throw new ExchangeException(-1, e.toString(), ExchangeException.NOT_FIXABLE);
        }
    }

    @Override
    public AccountInfo getAccountInfo(long timestamp) throws ExchangeException {

        LiveAccountInfo acc = new LiveAccountInfo();
        AccountInfoWrapper wrap = api.getAccountInfo();

        totalInitialMargin = wrap.getTotalInitialMargin();

        acc.marginBalance = wrap.getMarginBalance();
        acc.walletBalance = wrap.getWalletBalance();
        acc.leverages = wrap.getLeverages();
        acc.longPositions = wrap.getLongPositions();
        acc.shortPositions = wrap.getShortPositions();
        acc.isHedgeMode = wrap.isHedgeMode();

        exchangeInfo = api.getExchangeInfo();
        acc.exchangeTime = exchangeInfo.getExchangeTime();

        return acc;
    }

    @Override
    public void setHedgeMode() throws ExchangeException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLeverage(String symbol, int leverage) throws ExchangeException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setCrossMargin(String symbol) throws ExchangeException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addBatchMarketOpen(String orderId, String symbol, boolean isLong, double symbolQty)
            throws ExchangeException {
        if (totalInitialMargin == -1) {
            throw new IllegalStateException(
                    "Must successfully call getAccountInfo() at least once per LiveExchange instance.");
        }
        // TODO Auto-generated method stub

    }

    @Override
    public void addBatchMarketClose(String orderId, String symbol, boolean isLong, double qtyToClose)
            throws ExchangeException {
        if (totalInitialMargin == -1) {
            throw new IllegalStateException(
                    "Must successfully call getAccountInfo() at least once per LiveExchange instance.");
        }
        // TODO Auto-generated method stub

    }

    @Override
    public AccountInfo executeBatchedMarketOpenOrders() throws ExchangeException {
        // totalInitialMargin =
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AccountInfo executeBatchedMarketCloseOrders() throws ExchangeException {
        // totalInitialMargin =
        // TODO Auto-generated method stub
        return null;
    }

}
