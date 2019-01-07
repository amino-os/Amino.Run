package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;
import sapphire.sysSapphireObjects.metricCollector.MetricWithSelector;

public class GaugeMetricAggregator implements MetricWithSelector {
    private static Logger logger = Logger.getLogger(GaugeMetricAggregator.class.getName());
    private String metricName;
    private float value;
    private Labels labels;

    public GaugeMetricAggregator(String metricName, Labels labels) {
        this.metricName = metricName;
        this.labels = labels;
    }

    @Override
    public Metric getMetric(MetricSelector metricSelector) {
        GaugeMetric gaugeMetric =
                GaugeMetric.newBuilder()
                        .setMetricName(metricName)
                        .setLabels(labels)
                        .setValue(value)
                        .create();
        return gaugeMetric;
    }

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + value + ">";
    }

    public void merge(GaugeMetric metric) {
        synchronized (this) {
            value = metric.getValue();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
