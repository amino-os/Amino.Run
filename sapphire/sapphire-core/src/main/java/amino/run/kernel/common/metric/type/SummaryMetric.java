package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import java.util.HashMap;

public class SummaryMetric implements Metric {
    private String metricName;
    private HashMap<String, String> labels;
    private int observationCount;
    private long observationSum;
    private long time;

    public SummaryMetric(
            String metricName,
            HashMap<String, String> labels,
            long observationSum,
            int observationCount) {
        this.observationSum = observationSum;
        this.metricName = metricName;
        this.labels = labels;
        this.observationCount = observationCount;
        time = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public HashMap<String, String> getLabels() {
        return labels;
    }

    public int getObservationCount() {
        return observationCount;
    }

    @Override
    public boolean isEmpty() {
        return observationCount == 0;
    }

    public String toString() {
        return "<" + metricName + ":" + labels + ": observation sum " + observationSum + ">";
    }

    public long getTime() {
        return time;
    }
}
