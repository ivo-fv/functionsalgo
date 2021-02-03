package functionsalgo.shared;

public interface Strategy {

    TradeStatistics execute(long timestamp);

    boolean isLive();

}
