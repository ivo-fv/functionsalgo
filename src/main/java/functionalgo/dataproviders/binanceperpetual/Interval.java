package functionalgo.dataproviders.binanceperpetual;

public enum Interval {
    _1m {
        
        @Override
        public String toString() {
            
            return "1m";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 60000;
        }
    },
    _3m {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _5m {
        
        @Override
        public String toString() {            
            
            return "5m";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 300000;
        }
    }, _15m {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _30m {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _1h {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _2h {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _4h {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _6h {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _8h {
        
        @Override
        public String toString() {
            
            return "8h";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 28800000;
        }
    }, _12h {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _1d {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _3d {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _1w {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    }, _1M {
        
        @Override
        public String toString() {
            
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public long toMilliseconds() {
            
            // TODO Auto-generated method stub
            return 0;
        }
    };
    
    public abstract String toString();
    
    public abstract long toMilliseconds();
    
    public static Interval parseString(String string) {
        
        return Interval.valueOf("_" + string);
    }
}
