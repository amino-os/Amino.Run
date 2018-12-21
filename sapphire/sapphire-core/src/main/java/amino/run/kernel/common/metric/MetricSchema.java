package amino.run.kernel.common.metric;

import java.io.Serializable;
import java.util.HashMap;

/** Interface used in metric metadata information communication with metric server */
public interface MetricSchema extends Serializable {
    /**
     * Return Metric name
     *
     * @return name
     */
    String getName();

    /**
     * Return Metric labels
     *
     * @return labels
     */
    HashMap<String, String> getLabels();

    /**
     * Return kind of Metric.
     *
     * <p>Type of metric determine format of Metric getting send and stored. Counter, Gauge,
     * Histogram or Summary are some example of types. Metric server creates Object to store the
     * incoming Metric information depending on Metric type.
     *
     * @return
     */
    String getMetricType();
}
