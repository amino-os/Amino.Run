package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import java.util.LinkedHashMap;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class HistogramMetricAggregator implements Metric {
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
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    public void merge(HistogramMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            observedValues = metric.getValue();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
