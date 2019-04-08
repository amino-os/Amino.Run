package amino.run.kernel.common.metric;

import java.io.Serializable;
import java.util.HashMap;

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
    String getName();

    /**
     * Return metric labels
     *
     * @return labels
     */
    HashMap<String, String> getLabels();
}
