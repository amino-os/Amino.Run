package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;

import java.util.HashMap;

/** Counter metric reporting class */
public class CounterMetric implements Metric {
    private Schema schema;
    private long count;
    private long time;

    public CounterMetric(Schema schema, long count) {
        this.count = count;
        this.schema = schema;
        time = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return schema.getName();
    }

    @Override
    public HashMap<String, String> getLabels() {
        return schema.getLabels();
    }

    @Override
    public String toString() {
        return "<" + schema.getName() + ":" + schema.getLabels() + ":" + count + ">";
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
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
