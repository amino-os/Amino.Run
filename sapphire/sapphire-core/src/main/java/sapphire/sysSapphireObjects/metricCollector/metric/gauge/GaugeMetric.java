package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class GaugeMetric implements Metric {
    private String metricName;
    private float value;
    private Labels labels;

    public GaugeMetric(String metricName, Labels labels) {
        this.metricName = metricName;
        this.labels = labels;
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

    public void merge(GaugeMetric metric) {
        value = value + metric.getValue();
    }
}
