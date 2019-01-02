package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;

public class GaugeClientMetric implements Metric {
    private String metricName;
    private float value;
    private Labels labels;
    private MetricCollector collectorStub;

    public GaugeClientMetric(String metricName, Labels labels, MetricCollector metricCollector) {
        this.metricName = metricName;
        this.labels = labels;
        this.collectorStub = metricCollector;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float setVal) {
        value = (value + setVal) / 2;
    }

    public void clear() {
        value = 0f;
    }

    public Labels getLabels() {
        return labels;
    }

    public void sendMetric() throws Exception {
        collectorStub.push(this);
    }

    public static GaugeClientMetric.Builder newBuilder() {
        return new GaugeClientMetric.Builder();
    }

    public static class Builder {
        private String metricName;
        private Labels labels;
        private MetricCollector metricCollector;

        public GaugeClientMetric.Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public GaugeClientMetric.Builder setLabels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public GaugeClientMetric.Builder setMetricCollector(MetricCollector metricCollector) {
            this.metricCollector = metricCollector;
            return this;
        }

        public GaugeClientMetric create() {
            return new GaugeClientMetric(metricName, labels, metricCollector);
        }
    }
}
