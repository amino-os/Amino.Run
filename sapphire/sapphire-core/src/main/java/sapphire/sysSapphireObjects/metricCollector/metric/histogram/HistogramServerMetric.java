package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import java.util.LinkedHashMap;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class HistogramServerMetric implements Metric {
    private String metricName;
    private LinkedHashMap<Long, Object> observedValues;
    private Labels labels;

    public HistogramServerMetric(String metricName, Labels labels) {
        this.metricName = metricName;
        this.labels = labels;
        this.observedValues = new LinkedHashMap<>();
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    public void merge(HistogramClientMetric metric) {
        observedValues = metric.getValue();
    }
}
