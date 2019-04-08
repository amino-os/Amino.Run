package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.schema.Schema;
import java.io.Serializable;

/**
 * Class used for metric reporting
 *
 * @author AmitRoushan
 */
public class Metric implements Serializable {
    protected Schema schema;

    protected Metric(Schema schema) {
        this.schema = schema;
    }
    /**
     * Return metric name
     *
     * @return name
     */
    public Schema getSchema() {
        return schema;
    }
}
