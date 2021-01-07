package functionalgo.exchanges.binanceperpetual;

import java.util.ArrayList;

public class BPSimTradeExecutor implements BPTradeExecutor {
    
    private interface Order {
        
        void execute();
    }
    
    ArrayList<Order> orders;
    BPExchange exchange;
    
    public BPSimTradeExecutor(BPExchange exchange) {
        
        orders = new ArrayList<>();
        this.exchange = exchange;
    }
    
    @Override
    public void execute() {
        
        for (Order order : orders) {
            order.execute();
        }
    }
    
    @Override
    public void marketOpen(String positionId, String symbol, boolean isLong, double symbolQty) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketOpen(positionId, symbol, isLong, symbolQty);
            }
        };
        
        orders.add(order);
    }
    
    @Override
    public void marketClose(String positionId, double qtyToClose) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketClose(positionId, qtyToClose);
            }
        };
        
        orders.add(order);
    }
}
