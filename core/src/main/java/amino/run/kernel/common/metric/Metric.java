package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.schema.Schema;
import java.io.Serializable;

/**
 * Base metric class
 *
 * @author AmitRoushan
 */
public class Metric implements Serializable {
    private Schema schema;
    private long time;

    protected Metric(Schema schema) {
        this.schema = schema;
        time = System.currentTimeMillis();
    }

    /**
     * Return Metric collection time in milli seconds
     *
     * @return time
     */
    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return schema.toString();
    }
}
