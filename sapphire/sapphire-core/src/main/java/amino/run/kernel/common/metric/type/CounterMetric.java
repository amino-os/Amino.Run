package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import java.util.HashMap;

/** Counter metric reporting class */
public class CounterMetric extends MetricValue implements Metric {
    private String metricName;
    private HashMap<String, String> labels;

    public CounterMetric(String metricName, HashMap<String, String> labels, long count) {
        super(count, System.currentTimeMillis());
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

    public String toString() {
        return "<" + metricName + ":" + labels + ":" + value + ">";
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }
}
