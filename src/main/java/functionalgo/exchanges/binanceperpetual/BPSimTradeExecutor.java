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
    public void marketOpenLong(String symbol, double symbolQty) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketOpenLong(symbol, symbolQty);
            }
        };
        
        orders.add(order);
    }
    
    @Override
    public void marketOpenShort(String symbol, double symbolQty) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketOpenShort(symbol, symbolQty);
            }
        };
        
        orders.add(order);
    }
    
    @Override
    public void marketCloseLong(String symbol) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketCloseLong(symbol);
            }
        };
        
        orders.add(order);
    }
    
    @Override
    public void marketCloseShort(String symbol) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketCloseShort(symbol);
            }
        };
        
        orders.add(order);
    }
    
    @Override
    public void marketReduceLong(String symbol, double symbolQty) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketReduceLong(symbol, symbolQty);
            }
        };
        
        orders.add(order);
    }
    
    @Override
    public void marketReduceShort(String symbol, double symbolQty) {
        
        Order order = new Order() {
            
            @Override
            public void execute() {
                
                exchange.marketReduceShort(symbol, symbolQty);
            }
        };
        
        orders.add(order);
    }
}
