package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;
import java.util.HashMap;

/**
 * Class used for Summary metric type reporting. It maintains sum of all metric observations with
 * observation count.
 *
 * @author AmitROushan
 */
public class Summary implements Metric {
    private Schema schema;
    private int observationCount;
    private long observationSum;
    private long time;

    public Summary(Schema schema, long observationSum, int observationCount) {
        this.observationSum = observationSum;
        this.schema = schema;
        this.observationCount = observationCount;
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

    public int getObservationCount() {
        return observationCount;
    }

    @Override
    public boolean isEmpty() {
        return observationCount == 0;
    }

    public String toString() {
        return "<" + getName() + ":" + getLabels() + ": observation sum " + observationSum + ">";
    }

    public long getTime() {
        return time;
    }
}
