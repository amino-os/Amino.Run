package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class GaugeServerMetric implements Metric {
    private String metricName;
    private float value;
    private Labels labels;

    public GaugeServerMetric(String metricName, Labels labels) {
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

    public void merge(GaugeClientMetric metric) {
        value = metric.getValue();
    }
}
