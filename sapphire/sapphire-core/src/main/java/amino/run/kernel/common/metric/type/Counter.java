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

    public Counter(Schema schema, long count) {
        super(schema);
        this.count = count;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + "count=" + count;
    }
}
