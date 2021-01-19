package functionalgo;

import java.util.ArrayList;
import java.util.List;

public class BacktestLogger implements Logger {
    
    private static final String SEP = " ; ";
    
    List<String> logs;
    
    public BacktestLogger() {
        
        logs = new ArrayList<>();
    }
    
    @Override
    public void log(int level, int code, String codeMsgOrCause, String msgOrTrace) {
        
        logs.add(new String(level + SEP + code + SEP + codeMsgOrCause + SEP + msgOrTrace));
    }
    
    @Override
    public List<String> getLogs() {
        
        return logs;
    }
    
}
