package amino.run.kernel.common.metric;

import java.io.Serializable;
import java.util.HashMap;

/** Interface used for metric reporting */
public interface Metric extends Serializable {
    /**
     * Return name of Metric
     *
     * @return name
     */
    String getName();

    /**
     * Return labels of metric
     *
     * @return labels
     */
    HashMap<String, String> getLabels();

    /**
     * Return true if metric empty
     *
     * @return status
     */
    boolean isEmpty();
}
