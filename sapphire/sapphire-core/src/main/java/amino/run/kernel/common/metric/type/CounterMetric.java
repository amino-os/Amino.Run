package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import java.util.HashMap;

/** Counter metric reporting class */
public class CounterMetric implements Metric {
    private String metricName;
    private HashMap<String, String> labels;
    private long count;
    private long time = System.currentTimeMillis();

    public CounterMetric(String metricName, HashMap<String, String> labels, long count) {
        this.count = count;
        this.metricName = metricName;
        this.labels = labels;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public HashMap<String, String> getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        return "<" + metricName + ":" + labels + ":" + count + ">";
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Return Metric collection time in milli seconds
     *
     * @return time
     */
    public long getTime() {
        return time;
    }
}
