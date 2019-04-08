package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;

/**
 * Class used for Counter metric type reporting.
 *
 * @author AmitROushan
 */
public class Counter extends Metric {
    private long count;
    private long time;

    public Counter(Schema schema, long count) {
        super(schema);
        this.count = count;
        time = System.currentTimeMillis();
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
