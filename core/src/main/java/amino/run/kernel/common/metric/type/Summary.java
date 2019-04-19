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

    public Summary(Schema schema, long observationSum, int observationCount) {
        super(schema);
        this.observationSum = observationSum;
        this.observationCount = observationCount;
    }

    /**
     * Get the observation count value
     *
     * @return observationCount
     */
    public int getObservationCount() {
        return observationCount;
    }

    /**
     * Get the observation summary value
     *
     * @return observationSum
     */
    public long getObservationSum() {
        return observationSum;
    }

    @Override
    public String toString() {
        return super.toString()
                + ": "
                + "observationCount="
                + observationCount
                + " "
                + "observationSum="
                + observationSum;
    }
}
