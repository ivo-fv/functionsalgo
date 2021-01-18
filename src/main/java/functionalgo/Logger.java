package functionalgo;

public interface Logger {
    
    void log(int level, int code, String codeMsgOrCause, String msgOrTrace);
}
