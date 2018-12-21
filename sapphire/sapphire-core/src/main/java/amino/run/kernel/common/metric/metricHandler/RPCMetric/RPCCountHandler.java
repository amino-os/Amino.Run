package amino.run.kernel.common.metric.metricHandler.RPCMetric;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.kernel.common.metric.schema.SchemaType;
import amino.run.kernel.common.metric.type.CounterMetric;
import amino.run.kernel.common.metric.type.EmptyMetric;
import java.util.ArrayList;
import java.util.HashMap;

/** Counter is a cumulative metric that represents a single monotonically increasing counter. */
public class RPCCountHandler implements RPCMetricHandler {
    public static String METRIC_NAME = "rpc_count";
    private long count = 0;
    private transient Schema schema;

    @Override
    public String toString() {
        return METRIC_NAME + "<" + schema.getLabels().toString() + ":" + count + ">";
    }

    public RPCCountHandler(HashMap<String, String> labels) {
        schema = new Schema(METRIC_NAME, labels, SchemaType.CounterMetric);
    }

    @Override
    public void handle(String method, ArrayList<Object> params) {
        synchronized (this) {
            count++;
        }
    }

    @Override
    public Metric getMetric() {
        synchronized (this) {
            return new CounterMetric(schema, count);
        }
    }

    @Override
    public Schema getSchema() {
        return schema;
    }
}
