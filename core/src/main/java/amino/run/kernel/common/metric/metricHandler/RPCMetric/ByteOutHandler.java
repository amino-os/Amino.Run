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
 * RPC metric handler for collecting average metric response data over RPC.
 *
 * @author AmitRoushan
 */
public class ByteOutHandler extends RPCMetricHandler {
    public static final String METRIC_NAME = "avg_byte_out_data";
    private long totalByteOut = 0;
    private int rpcCount = 0;
    private transient Schema schema;

    public ByteOutHandler(MicroServiceMetricManager manager) {
        super(manager);
        schema = new Schema(METRIC_NAME, SchemaType.Summary);
    }

    @Override
    public Object handle(String method, ArrayList<Object> params) throws Exception {
        Object object = getNextHandler().handle(method, params);
        synchronized (this) {
            totalByteOut += sizeOf(object);
            rpcCount++;
        }
        return object;
    }

    @Override
    public Metric getMetric() {
        synchronized (this) {
            Summary metric = new Summary(schema, totalByteOut, rpcCount);
            totalByteOut = 0;
            rpcCount = 0;
            return metric;
        }
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    private int sizeOf(Object object) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(object);
        out.flush();
        /*  Subtract 4 bytes from the length, because the serialization
        magic number (2 bytes) and version number (2 bytes) are
        both written to the stream before the object
        */
        return baos.toByteArray().length - 4;
    }
}
