package amino.run.kernel.common.metric.metricHandler.RPCMetric;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.metricHandler.MicroServiceMetricManager;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.kernel.common.metric.schema.SchemaType;
import amino.run.kernel.common.metric.type.Summary;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * RPC metric handler for collecting average metric request data over RPC.
 *
 * @author AmitRoushan
 */
public class ByteInHandler extends RPCMetricHandler {
    public static final String METRIC_NAME = "avg_byte_in_data";
    private long totalByteIn = 0;
    private int rpcCount = 0;
    private transient Schema schema;

    public ByteInHandler(MicroServiceMetricManager manager) {
        super(manager);
        schema = new Schema(METRIC_NAME, SchemaType.Summary);
    }

    @Override
    public Object handle(String method, ArrayList<Object> params) throws Exception {
        synchronized (this) {
            totalByteIn += sizeOf(method, params);
            rpcCount++;
        }
        return getNextHandler().handle(method, params);
    }

    @Override
    public Metric getMetric() {
        synchronized (this) {
            Summary metric = new Summary(schema, totalByteIn, rpcCount);
            totalByteIn = 0;
            rpcCount = 0;
            return metric;
        }
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    private int sizeOf(String method, ArrayList<Object> params) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
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
