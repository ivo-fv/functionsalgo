package functionsalgo.samplestrat;

import java.io.IOException;

public interface Storage {

    State getCurrentState();

    void saveCurrentState(State state) throws IOException;

}
