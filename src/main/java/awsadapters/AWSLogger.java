package awsadapters;

import functionalgo.Logger;

public class AWSLogger implements Logger {
    
    private static final boolean IS_TEST = false;
    
    @Override
    public void log(int level, int code, String codeMsg, String msg) {
        
        if (IS_TEST) {
            System.out.println(level + ";" + code + ";" + codeMsg + ";" + msg);
        }
        
        // TODO implement to work with aws lambda
    }
}
