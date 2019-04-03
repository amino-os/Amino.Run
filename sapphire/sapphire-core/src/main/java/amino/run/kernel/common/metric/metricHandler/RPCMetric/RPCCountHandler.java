package amino.run.kernel.common.metric.metricHandler.RPCMetric;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.MetricSchema;
import amino.run.kernel.common.metric.metricHandler.RPCMetricHandler;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.kernel.common.metric.schema.SchemaType;
import amino.run.kernel.common.metric.type.CounterMetric;
import amino.run.kernel.common.metric.type.EmptyMetric;
import java.util.ArrayList;
import java.util.HashMap;

/** Counter is a cumulative metric that represents a single monotonically increasing counter. */
public class RPCCountHandler extends RPCMetricHandler {
    public static transient String metricName = "rpc_count";
    private long count = 0;
    private transient HashMap<String, String> labels;
    private boolean refreshed;

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + count + ">";
    }

    public RPCCountHandler(HashMap<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public Object handle(String method, ArrayList<Object> params) throws Exception {
        synchronized (this) {
            refreshed = true;
            count++;
        }
        return getNextHandler().handle(method, params);
    }

    @Override
    public Metric getMetric() {
        synchronized (this) {
            if (!refreshed) {
                return new EmptyMetric();
            }

            refreshed = false;
            return new CounterMetric(metricName, labels, count);
        }
    }

    @Override
    public MetricSchema getSchema() {
        return new Schema(metricName, labels, SchemaType.CounterMetric);
    }
}
