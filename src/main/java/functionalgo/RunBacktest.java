package functionalgo;

public class RunBacktest {
    
    public static void main(String[] args) {
        
        // TODO set times for a backtest run, get the interval from candles (klines) from an Exchange
        long initialTime = 0;
        long endTime = 0;
        long interval = 0;
        
        for (long t = initialTime; t < endTime; t += interval) {
            
            Function.run(t); // maybe a liveRun , backtestRun or BacktestFunction , Live Function
            
        }
        
    }
    
}
