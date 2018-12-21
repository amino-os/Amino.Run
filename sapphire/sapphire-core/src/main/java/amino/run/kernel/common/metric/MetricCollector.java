package amino.run.kernel.common.metric;

import java.io.Serializable;

/** */
public interface MetricCollector extends Serializable {
    /**
     * Return latest collected metric data
     *
     * @return metric
     */
    Metric getMetric();

    /**
     * Return schema of Metrics collected
     *
     * @return schema
     */
    MetricSchema getSchema();
}
