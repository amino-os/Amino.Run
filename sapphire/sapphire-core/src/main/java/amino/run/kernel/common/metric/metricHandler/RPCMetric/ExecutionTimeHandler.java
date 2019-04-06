package amino.run.kernel.common.metric.metricHandler.RPCMetric;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.kernel.common.metric.schema.SchemaType;
import amino.run.kernel.common.metric.type.Summary;
import amino.run.policy.DefaultPolicy;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * RPC metric collector which collects execution time metric.
 *
 * <p>This should be last RPC metric handler in chain as this performs actual onRPC call. Execution
 * time metric collection can be enabled with {@link
 * amino.run.kernel.common.metric.MetricConstants}.METRIC_LIST.
 *
 * @author AmitRoushan
 */
public class ExecutionTimeHandler extends RPCMetricHandler {
    public static final String METRIC_NAME = "avg_execution_time";
    private transient DefaultPolicy.DefaultServerPolicy policy;
    private long totalExecutionTime = 0;
    private int rpcCount = 0;
    private transient boolean enabled;
    private transient Schema schema;

    public ExecutionTimeHandler(
            HashMap<String, String> labels,
            DefaultPolicy.DefaultServerPolicy policy,
            boolean enabled) {
        this.policy = policy;
        this.enabled = enabled;
        this.schema = new Schema(METRIC_NAME, labels, SchemaType.Summary);
    }

    @Override
    public Object handle(String method, ArrayList<Object> params) throws Exception {
        if (!enabled) {
            return policy.upRPCCall(method, params);
        }
        long startTime = System.nanoTime();
        Object object = policy.upRPCCall(method, params);
        synchronized (this) {
            totalExecutionTime += System.nanoTime() - startTime;
            rpcCount++;
        }
        return object;
    }

    @Override
    public Metric getMetric() {
        synchronized (this) {
            Summary metric = new Summary(schema, totalExecutionTime, rpcCount);
            totalExecutionTime = 0;
            rpcCount = 0;
            return metric;
        }
    }

    @Override
    public Schema getSchema() {
        return schema;
    }
}
