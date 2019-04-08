package amino.run.kernel.common.metric.schema;

/**
 * Class used for reporting metric MetaData information.
 *
 * @author AmitRoushan
 */
public class Schema {
    private String metricName;
    private SchemaType type;

    public Schema(String metricName, SchemaType type) {
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

    @Override
    public String toString() {
        return "metricName=" + metricName + ": " + "type=" + type;
    }

    /**
     * Return kind of Metric.
     *
     * <p>Type of metric determine format of Metric getting send and stored. Counter, Gauge,
     * Histogram or Summary are some example of types. Metric server creates Object to store the
     * incoming Metric information depending on Type.
     *
     * @return
     */
    public String getType() {
        return type.toString();
    }
}
