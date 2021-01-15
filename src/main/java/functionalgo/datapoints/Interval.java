package functionalgo.datapoints;

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
            
            return "3m";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 180000;
        }
    },
    _5m {
        
        @Override
        public String toString() {
            
            return "5m";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 300000;
        }
    },
    _15m {
        
        @Override
        public String toString() {
            
            return "15m";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 900000;
        }
    },
    _30m {
        
        @Override
        public String toString() {
            
            return "30m";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 1800000;
        }
    },
    _1h {
        
        @Override
        public String toString() {
            
            return "1h";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 3600000;
        }
    },
    _2h {
        
        @Override
        public String toString() {
            
            return "2h";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 7200000;
        }
    },
    _4h {
        
        @Override
        public String toString() {
            
            return "4h";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 14400000;
        }
    },
    _6h {
        
        @Override
        public String toString() {
            
            return "6h";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 21600000;
        }
    },
    _8h {
        
        @Override
        public String toString() {
            
            return "8h";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 28800000;
        }
    },
    _12h {
        
        @Override
        public String toString() {
            
            return "12h";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 43200000;
        }
    },
    _1d {
        
        @Override
        public String toString() {
            
            return "1d";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 86400000;
        }
    },
    _3d {
        
        @Override
        public String toString() {
            
            return "3d";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 259200000;
        }
    },
    _1w {
        
        @Override
        public String toString() {
            
            return "1w";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 604800000;
        }
    },
    _1M {
        
        @Override
        public String toString() {
            
            return "1M";
        }
        
        @Override
        public long toMilliseconds() {
            
            return 2629800000L;
        }
    };
    
    public abstract String toString();
    
    public abstract long toMilliseconds();
    
    public static Interval parseString(String string) {
        
        return Interval.valueOf("_" + string);
    }
}
