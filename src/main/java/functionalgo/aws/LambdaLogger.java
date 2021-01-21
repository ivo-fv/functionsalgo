package functionalgo.aws;

import java.util.List;

import functionalgo.Logger;

public class LambdaLogger implements Logger {

    private boolean isTest;

    public LambdaLogger(boolean isTest) {
        this.isTest = isTest;
    }

    @Override
    public void log(int level, int code, String codeMsgOrCause, String msgOrTrace) {

        if (isTest) {
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
