package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;

/** Gauge metric reporting class */
public class Gauge extends Metric {
    private long value;

    public Gauge(Schema schema, long value) {
        super(schema);
        this.value = value;
    }

    /**
     * Get the gauge value
     *
     * @return value
     */
    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + value;
    }
}
