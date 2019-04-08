package amino.run.kernel.common.metric.schema;

import java.util.HashMap;

/**
 * Class used for reporting metric MetaData information.
 *
 * @author AmitRoushan
 */
public class Schema {
    private String metricName;
    private HashMap<String, String> labels;
    private SchemaType type;

    public Schema(String metricName, HashMap<String, String> labels, SchemaType type) {
        this.labels = labels;
        this.metricName = metricName;
        this.type = type;
    }

    /**
     * Return Metric name
     *
     * @return name
     */
    public String getName() {
        return metricName;
    }

    /**
     * Return Metric labels
     *
     * @return labels
     */
    public HashMap<String, String> getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        return "<" + metricName + ":" + labels + ">";
    }

    /**
     * Return kind of Metric.
     *
     * <p>Type of metric determine format of Metric getting send and stored. Counter, Gauge,
     * Histogram or Summary are some example of types. Metric server creates Object to store the
     * incoming Metric information depending on Metric type.
     *
     * @return
     */
    public String getMetricType() {
        return type.toString();
    }
}
