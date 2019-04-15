package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;

/** Gauge metric reporting class */
public class Gauge extends Metric {
    private Object value;

    public Gauge(Schema schema, Object value) {
        super(schema);
        this.value = value;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + value;
    }
}
