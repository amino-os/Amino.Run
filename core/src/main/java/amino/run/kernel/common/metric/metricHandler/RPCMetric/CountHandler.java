package amino.run.kernel.common.metric.metricHandler.RPCMetric;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.metricHandler.MicroServiceMetricManager;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.kernel.common.metric.schema.SchemaType;
import amino.run.kernel.common.metric.type.Counter;
import java.util.ArrayList;

/**
 * Counter is a cumulative metric that represents a single monotonically increasing counter.
 *
 * @author AmitRoushan
 */
public class CountHandler extends RPCMetricHandler {
    public static final String METRIC_NAME = "rpc_count";
    private long count = 0;
    private transient Schema schema;

    public CountHandler(MicroServiceMetricManager manager) {
        super(manager);
        schema = new Schema(METRIC_NAME, SchemaType.Counter);
    }

    @Override
    public Object handle(String method, ArrayList<Object> params) throws Exception {
        synchronized (this) {
            count++;
        }
        return getNextHandler().handle(method, params);
    }

    @Override
    public Metric getMetric() {
        synchronized (this) {
            return new Counter(schema, count);
        }
    }

    @Override
    public Schema getSchema() {
        return schema;
    }
}
