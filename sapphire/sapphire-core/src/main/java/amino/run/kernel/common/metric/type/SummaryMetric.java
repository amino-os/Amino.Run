package amino.run.kernel.common.metric.type;

import amino.run.kernel.common.metric.Metric;
import java.util.HashMap;

public class SummaryMetric extends MetricValue implements Metric {
    private String metricName;
    private HashMap<String, String> labels;
    private int observationCount;

    public SummaryMetric(
            String metricName,
            HashMap<String, String> labels,
            long observationSum,
            int observationCount) {
        super(observationSum, System.currentTimeMillis());
        this.metricName = metricName;
        this.labels = labels;
        this.observationCount = observationCount;
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

    public boolean isEmpty() {
        return observationCount == 0;
    }

    public String toString() {
        return "<" + metricName + ":" + labels + ":" + value + ">";
    }
}
