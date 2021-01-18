package functionalgo.awsadapters;

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
}
