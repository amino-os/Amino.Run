package amino.run.kernel.common.metric.metricHandler;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.MetricSchema;
import java.io.Serializable;

/** Interface to collect metric info from metric handler */
public interface MetricHandler extends Serializable {
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
