package functionsalgo.samplestrat;

import java.util.HashMap;
import java.util.Map;

public class State {
    private Map<Integer, Position> positions = new HashMap<>();

    public void addPosition(int id, Position pos) {
        positions.put(id, pos);
    }

    public void remove(int id) {
        positions.remove(id);
    }

    public Position getPosition(int id) {
        return positions.get(id);
    }

}
