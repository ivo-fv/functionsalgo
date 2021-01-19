package functionalgo;

import java.util.List;

public interface Logger {
    
    void log(int level, int code, String codeMsgOrCause, String msgOrTrace);
    
    List<String> getLogs();
}
