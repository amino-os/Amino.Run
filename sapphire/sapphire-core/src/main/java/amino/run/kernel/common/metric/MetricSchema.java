package amino.run.kernel.common.metric;

import java.io.Serializable;
import java.util.HashMap;

/** Interface used for Metric metadata replay to Metric server */
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
     * Histogram or WMA are some example of some type. With Type of metric Metric server creates
     * Object to store the incoming Metric information.
     *
     * @return
     */
    String getMetricType();
}
