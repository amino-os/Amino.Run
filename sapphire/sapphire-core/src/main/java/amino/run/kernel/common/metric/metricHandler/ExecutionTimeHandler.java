package amino.run.kernel.common.metric.metricHandler;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.MetricSchema;
import amino.run.kernel.common.metric.schema.SummarySchema;
import amino.run.kernel.common.metric.type.SummaryMetric;
import amino.run.policy.DefaultPolicy;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * RPC metric collector which collects execution time metric.
 *
 * <p>This should be last RPC metric handler in chain as this performs actual onRPC call. Execution
 * time metric collection can be enabled with {@link
 * amino.run.kernel.common.metric.MetricConstants}.METRIC_LIST.
 */
public class ExecutionTimeHandler extends RPCMetricHandler {
    public static transient String metricName = "avg_execution_time";
    private transient DefaultPolicy.DefaultServerPolicy policy;
    private long totalExecutionTime = 0;
    private int rpcCount = 0;
    private transient HashMap<String, String> labels;
    private transient boolean enabled;

    public ExecutionTimeHandler(
            HashMap<String, String> labels,
            DefaultPolicy.DefaultServerPolicy policy,
            boolean enabled) {
        this.policy = policy;
        this.labels = labels;
        this.enabled = enabled;
    }

    @Override
    public Object handle(String method, ArrayList<Object> params) throws Exception {
        if (!enabled) {
            return policy.getAppObject().invoke(method, params);
        }
        long startTime = System.nanoTime();
        Object object = policy.getAppObject().invoke(method, params);
        synchronized (this) {
            totalExecutionTime = System.nanoTime() - startTime;
            rpcCount++;
        }
        return object;
    }

    @Override
    public Metric getMetric() {
        if (!enabled || rpcCount == 0) {
            return null;
        }

        synchronized (this) {
            SummaryMetric metric =
                    new SummaryMetric(metricName, labels, totalExecutionTime, rpcCount);
            totalExecutionTime = 0;
            rpcCount = 0;
            return metric;
        }
    }

    @Override
    public MetricSchema getSchema() {
        return new SummarySchema(metricName, labels);
    }
}
