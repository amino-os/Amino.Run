package amino.run.kernel.common.metric;

import amino.run.kernel.common.metric.schema.Schema;
import java.io.Serializable;

/**
 * Interface used in metric reporting
 *
 * @author AmitRoushan
 */
public interface Metric extends Serializable {
    /**
     * Return metric name
     *
     * @return name
     */
    Schema getSchema();
}
