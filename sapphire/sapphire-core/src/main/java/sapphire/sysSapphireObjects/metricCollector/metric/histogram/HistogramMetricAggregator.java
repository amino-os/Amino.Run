package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import java.util.LinkedHashMap;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;
import sapphire.sysSapphireObjects.metricCollector.MetricWithSelector;

public class HistogramMetricAggregator implements MetricWithSelector {
    private static Logger logger = Logger.getLogger(HistogramMetricAggregator.class.getName());
    private String metricName;
    private LinkedHashMap<Long, Object> observedValues;
    private Labels labels;

    public HistogramMetricAggregator(String metricName, Labels labels) {
        this.metricName = metricName;
        this.labels = labels;
        this.observedValues = new LinkedHashMap<>();
    }

    @Override
    public Metric getMetric(MetricSelector metricSelector) {
        HistogramMetric histogramMetric =
                HistogramMetric.newBuilder()
                        .setMetricName(metricName)
                        .setLabels(labels)
                        .setvalues(observedValues)
                        .create();
        return histogramMetric;
    }

    public void merge(HistogramMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            observedValues = metric.getValue();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
