package amino.run.kernel.common.metric.metricHandler;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.MetricSchema;
import amino.run.kernel.common.metric.schema.SummarySchema;
import amino.run.kernel.common.metric.type.SummaryMetric;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/** RPC metric handler for collecting average metric data processes over RPC. */
public class RPCDataHandler extends RPCMetricHandler {
    public static transient String metricName = "avg_rpc_data";
    private long totalVolume = 0; // in byte
    private int rpcCount = 0;
    private transient HashMap<String, String> labels;

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + totalVolume + ">";
    }

    public RPCDataHandler(HashMap<String, String> labels) {
        this.labels = labels;
    }

    public Object handle(String method, ArrayList<Object> params) throws Exception {
        Object object = nextHandler.handle(method, params);
        synchronized (this) {
            totalVolume += sizeOf(method, params, object);
            rpcCount++;
        }
        return object;
    }

    @Override
    public Metric getMetric() {
        if (rpcCount == 0) {
            return null;
        }
        synchronized (this) {
            SummaryMetric metric = new SummaryMetric(metricName, labels, totalVolume, rpcCount);
            totalVolume = 0;
            rpcCount = 0;
            return metric;
        }
    }

    @Override
    public MetricSchema getSchema() {
        return new SummarySchema(metricName, labels);
    }

    private int sizeOf(String method, ArrayList<Object> params, Object object) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(object);
        out.writeObject(method);
        out.writeObject(params);
        out.flush();
        /*  Subtract 4 bytes from the length, because the serialization
        magic number (2 bytes) and version number (2 bytes) are
        both written to the stream before the object
        */
        return baos.toByteArray().length - 4;
    }
}
