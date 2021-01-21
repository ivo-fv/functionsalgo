package mocks;

import java.util.ArrayList;
import java.util.List;

import functionalgo.Logger;

public class MockLogger implements Logger {

    private List<String> logs = new ArrayList<>();

    @Override
    public void log(int level, int code, String codeMsgOrCause, String msgOrTrace) {

        logs.add(level + " || " + code + " || " + codeMsgOrCause + " || " + msgOrTrace);
        System.out.println(logs.get(logs.size() - 1));
    }

    @Override
    public List<String> getLogs() {
        return logs;
    }

}
