package functionalgo;

import java.util.List;

// TODO make easy to understand constants or enums for level and code
public interface Logger {
    
    void log(int level, int code, String codeMsgOrCause, String msgOrTrace);
    
    List<String> getLogs();
}
