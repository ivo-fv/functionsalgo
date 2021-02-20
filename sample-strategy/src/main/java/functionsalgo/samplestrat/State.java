package functionsalgo.samplestrat;

import java.util.HashMap;
import java.util.Map;

public class State {
    private Map<String, Position> positions = new HashMap<>();

    public void addPosition(String id, Position pos) {
        positions.put(id, pos);
    }

    public void remove(String id) {
        positions.remove(id);
    }

    public Position getPosition(String id) {
        return positions.get(id);
    }

}
