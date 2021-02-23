package functionsalgo.datapoints;

public final class Timestamp {
    private long time;
    private final Interval interval;

    private Timestamp(Interval interval) {
        this.interval = interval;
    }

    public Timestamp(long timestamp, Interval interval) {
        this.interval = interval;
        this.time = (timestamp / interval.toMilliseconds()) * interval.toMilliseconds();
    }

    public final long getTime() {
        return time;
    }

    /**
     * "Timestamp t1;" "t1.add(t2).add(t3);" is analogous to "t1 = t1 + t2 + t3;"
     * TODO correct for different Timestamp intervals so that this Timestamp stays
     * consistent
     * 
     * @param timestamp
     * @return this instance (the Timestamp instance to which timestamp was added
     *         to)
     */
    public final Timestamp add(Timestamp timestamp) {
        time += timestamp.time;
        return this;
    }

    public final Timestamp add(long timestamp) {
        time += timestamp;
        return this;
    }

    public final Timestamp inc() {
        time += interval.toMilliseconds();
        return this;
    }

    public final Timestamp sub(Timestamp timestamp) {
        time -= timestamp.time;
        return this;
    }

    public final Timestamp sub(long timestamp) {
        time -= timestamp;
        return this;
    }

    public final Timestamp dec() {
        time -= interval.toMilliseconds();
        return this;
    }

    public final Timestamp copy() {
        Timestamp newTime = new Timestamp(interval);
        newTime.time = time;
        return newTime;
    }

}
