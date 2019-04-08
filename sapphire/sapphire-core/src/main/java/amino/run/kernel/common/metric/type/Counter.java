package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;

/**
 * Class used for Counter metric type reporting.
 *
 * @author AmitROushan
 */
public class Counter implements Metric {
    private Schema schema;
    private long count;
    private long time;

    public Counter(Schema schema, long count) {
        this.count = count;
        this.schema = schema;
        time = System.currentTimeMillis();
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public String toString() {
        return "<" + schema.getName() + ":" + schema.getLabels() + ":" + count + ">";
    }

    /**
     * Return Metric collection time in milli seconds
     *
     * @return time
     */
    public long getTime() {
        return time;
    }
}
