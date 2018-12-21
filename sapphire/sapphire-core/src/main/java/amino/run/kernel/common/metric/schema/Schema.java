package amino.run.kernel.common.metric.schema;

import amino.run.kernel.common.metric.MetricSchema;
import java.util.HashMap;

public abstract class Schema implements MetricSchema {
    private String metricName;
    private HashMap<String, String> labels;

    public Schema(String metricName, HashMap<String, String> labels) {
        this.labels = labels;
        this.metricName = metricName;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public HashMap<String, String> getLabels() {
        return labels;
    }

    public abstract String getMetricType();

    public String toString() {
        return "<" + metricName + ":" + labels + ">";
    }
}
