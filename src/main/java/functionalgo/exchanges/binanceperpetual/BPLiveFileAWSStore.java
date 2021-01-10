package functionalgo.exchanges.binanceperpetual;

import functionalgo.exchanges.binanceperpetual.BPLiveExchange.AccountInfo;

public class BPLiveFileAWSStore implements BPLiveStore {
    
    @Override
    public AccountInfo getAccountInfo(String apiKey) {
        
        // TODO always retry some number of times if it fails
        // if it fails log
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void setAccountInfo(String apiKey,AccountInfo accountInfo) {
        
        // TODO maybe throw or return something in case it fails or maybe retry and finally fail after some tries
        // always retry some number of times if it fails
        // TODO Auto-generated method stub
        
    }
    
}
