package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.schema.Schema;
import java.util.ArrayList;
import java.util.HashMap;

public interface MetricManager {
    /**
     * Retrieve and return metric managed in metric handlers
     *
     * @return list of metrics
     */
    ArrayList<Metric> getMetrics();

    /**
     * Return metric retrieval frequency
     *
     * @return
     */
    long getMetricUpdateFrequency();

    /**
     * Return tags which should be supplied on each metric
     *
     * @return
     */
    HashMap<String, String> getLabels();

    /**
     * Return metric schema of all handlers managed by metric manager
     *
     * @return
     */
    ArrayList<Schema> getSchemas();
}
