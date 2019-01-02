package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import java.util.LinkedHashMap;
import java.util.Map;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;

public class HistogramClientMetric implements Metric {
    private String metricName;
    private Labels labels;
    private int bucketSize;
    private LinkedHashMap<Long, Object> observedValues;
    private LinkedHashMap<Long, Object> values;
    private int batchSize;
    private MetricCollector collectorStub;

    public HistogramClientMetric(
            String metricName,
            Labels labels,
            int bucketSize,
            int batchSize,
            MetricCollector metricCollector) {
        this.metricName = metricName;
        this.labels = labels;
        this.bucketSize = bucketSize;
        this.batchSize = batchSize;
        this.observedValues = new LinkedHashMap<>();
        this.collectorStub = metricCollector;
    }

    public HistogramClientMetric(
            String metricName, Labels labels, LinkedHashMap<Long, Object> values) {
        this.metricName = metricName;
        this.labels = labels;
        this.values = values;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    public void sendMetric() throws Exception {
        for (Map.Entry<Long, Object> entry : observedValues.entrySet()) {
            if (values.size() == batchSize) {
                HistogramClientMetric obj = new HistogramClientMetric(metricName, labels, values);
                collectorStub.push(obj);
                values.clear();
            } else {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        if (values.size() != 0) {
            HistogramClientMetric obj = new HistogramClientMetric(metricName, labels, values);
            collectorStub.push(obj);
        }
    }

    public void reset() {
        observedValues.clear();
    }

    public void setValue(long time, Object value) {
        observedValues.put(time, value);
    }

    public Labels getLabels() {
        return labels;
    }

    public LinkedHashMap<Long, Object> getValue() {
        return observedValues;
    }

    public static class Builder {
        private int bucketSize;
        private int batchSize;
        private String metricName;
        private Labels labels;
        private MetricCollector metricCollector;

        public HistogramClientMetric.Builder setBucketSize(int bucketSize) {
            this.bucketSize = bucketSize;
            return this;
        }

        public HistogramClientMetric.Builder setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public HistogramClientMetric.Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public HistogramClientMetric.Builder setLabels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public HistogramClientMetric.Builder setMetricCollector(MetricCollector metricCollector) {
            this.metricCollector = metricCollector;
            return this;
        }

        public HistogramClientMetric create() {
            return new HistogramClientMetric(
                    metricName, labels, bucketSize, batchSize, metricCollector);
        }
    }
}
