package functionsalgo.samplestrat;

import java.io.IOException;

public class BacktestStorage implements Storage {

    private final State state;

    public BacktestStorage(State initialState) {
        state = initialState;
    }

    @Override
    public State getCurrentState() {
        return state;
    }

    @Override
    public void saveCurrentState(State state) throws IOException {
        // no need for backtest to implement this
    }

}
