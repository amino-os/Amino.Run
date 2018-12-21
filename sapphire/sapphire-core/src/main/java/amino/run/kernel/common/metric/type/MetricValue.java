package amino.run.kernel.common.metric.type;

/** Class maintains Metric data and time */
public class MetricValue {
    protected Object value;
    protected long time;

    public MetricValue(Object value, long time) {
        this.time = time;
        this.value = value;
    }
    /**
     * return metric data
     *
     * @return
     */
    public Object getValue() {
        return value;
    }

    /**
     * return time
     *
     * @return
     */
    public long getTime() {
        return time;
    }
}
