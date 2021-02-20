package functionsalgo.shared;

public interface Strategy {

    Statistics execute(long timestamp);

    boolean isLive();

}
