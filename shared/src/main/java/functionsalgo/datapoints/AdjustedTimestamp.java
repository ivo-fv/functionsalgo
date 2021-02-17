package functionsalgo.datapoints;

public final class AdjustedTimestamp {
    private long time;
    private final Interval interval;

    public AdjustedTimestamp(long timestamp, Interval interval) {
        this.interval = interval;
        this.time = (timestamp / interval.toMilliseconds()) * interval.toMilliseconds();
    }

    public final long getTime() {
        return time;
    }

    /**
     * "AdjustedTimestamp t1;" "t1.add(t2).add(t3);" is analogous to "t1 = t1 + t2 +
     * t3;"
     * 
     * @param timestamp
     * @return this instance (the AdjustedTimestamp instance to which timestamp was
     *         added to)
     */
    public final AdjustedTimestamp add(AdjustedTimestamp timestamp) {
        time += timestamp.time;
        return this;
    }

    public final AdjustedTimestamp inc() {
        time += interval.toMilliseconds();
        return this;
    }

    public final AdjustedTimestamp sub(AdjustedTimestamp timestamp) {
        time -= timestamp.time;
        return this;
    }

    public final AdjustedTimestamp dec() {
        time -= interval.toMilliseconds();
        return this;
    }

}
