package amino.run.kernel.common.metric.schema;

import amino.run.kernel.common.metric.MetricSchema;
import java.util.HashMap;

/** Class used for reporting metric meta data information with metric server */
public class Schema implements MetricSchema {
    private String metricName;
    private HashMap<String, String> labels;
    SchemaType type;

    public Schema(String metricName, HashMap<String, String> labels, SchemaType type) {
        this.labels = labels;
        this.metricName = metricName;
        this.type = type;
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
        return "<" + metricName + ":" + labels + ">";
    }

    @Override
    public String getMetricType() {
        return type.toString();
    }
}
