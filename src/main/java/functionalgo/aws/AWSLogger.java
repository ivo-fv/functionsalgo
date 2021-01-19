package functionalgo.aws;

import java.util.List;

import functionalgo.Logger;

public class AWSLogger implements Logger {
    
    private static final boolean IS_TEST = false;
    
    @Override
    public void log(int level, int code, String codeMsgOrCause, String msgOrTrace) {
        
        if (IS_TEST) {
            System.out.println(level + "  ;  " + code + "  ;  " + codeMsgOrCause + "  ;  " + msgOrTrace);
        }
        
        // TODO implement to work with aws lambda
    }
    
    @Override
    public List<String> getLogs() {
        
        // TODO Auto-generated method stub
        return null;
    }
}
