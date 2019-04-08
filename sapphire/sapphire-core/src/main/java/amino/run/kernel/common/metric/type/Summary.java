package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.schema.Schema;

/**
 * Class used for Summary metric type reporting. It maintains sum of all metric observations with
 * observation count.
 *
 * @author AmitROushan
 */
public class Summary extends Metric {
    private int observationCount;
    private long observationSum;
    private long time;

    public Summary(Schema schema, long observationSum, int observationCount) {
        super(schema);
        this.observationSum = observationSum;
        this.observationCount = observationCount;
        time = System.currentTimeMillis();
    }

    public int getObservationCount() {
        return observationCount;
    }

    public String toString() {
        return "<"
                + schema.getName()
                + ":"
                + schema.getLabels()
                + ": observation sum "
                + observationSum
                + ">";
    }

    public long getTime() {
        return time;
    }
}
