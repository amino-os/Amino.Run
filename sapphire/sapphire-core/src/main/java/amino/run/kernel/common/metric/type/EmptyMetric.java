package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import java.util.HashMap;

/** Metric handler uses this class instance to report no metric data collection. */
public class EmptyMetric implements Metric {
    @Override
    public String getName() {
        return "EmptyMetric";
    }

    @Override
    public HashMap<String, String> getLabels() {
        return new HashMap<String, String>();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
