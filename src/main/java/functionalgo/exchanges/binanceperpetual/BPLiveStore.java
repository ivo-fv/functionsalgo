package functionalgo.exchanges.binanceperpetual;

import functionalgo.exchanges.binanceperpetual.BPLiveExchange.AccountInfo;

public interface BPLiveStore {

    AccountInfo getAccountInfo();

    void setAccountInfo(AccountInfo accountInfo);
    
}
