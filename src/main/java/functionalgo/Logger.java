package functionalgo;

import java.util.List;

//TODO make singleton logger static first time it is invoked
// TODO make easy to understand constants or enums for level and code
public interface Logger {

    void log(int level, int code, String codeMsgOrCause, String msgOrTrace);

    List<String> getLogs();
}
