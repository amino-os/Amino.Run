package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import java.util.HashMap;

/** Gauge metric reporting class */
public class GaugeMetric extends MetricValue implements Metric {
    private String metricName;
    private HashMap<String, String> labels;

    public GaugeMetric(String metricName, HashMap<String, String> labels, Object value) {
        super(value, System.currentTimeMillis());
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

    public boolean isEmpty() {
        return value == null;
    }

    public String toString() {
        return "<" + metricName + ":" + labels + ":" + value + ">";
    }
}
