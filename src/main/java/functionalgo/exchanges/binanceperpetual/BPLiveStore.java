package functionalgo.exchanges.binanceperpetual;

import functionalgo.exchanges.binanceperpetual.BPLiveExchange.AccountInfo;

public interface BPLiveStore {
    
    AccountInfo getAccountInfo(String apiKey);
    
    void setAccountInfo(String apiKey, AccountInfo accountInfo);
    
}
