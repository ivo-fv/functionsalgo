package functionsalgo.shared;

public interface BacktestConfiguration {

    void generateConfiguration();

    void loadConfiguration();

    Strategy getStrategy();
}
