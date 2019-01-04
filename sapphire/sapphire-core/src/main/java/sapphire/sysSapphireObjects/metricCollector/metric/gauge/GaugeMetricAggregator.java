package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class GaugeMetricAggregator implements Metric {
    private static Logger logger = Logger.getLogger(GaugeMetricAggregator.class.getName());
    private String metricName;
    private float value;
    private Labels labels;

    public GaugeMetricAggregator(String metricName, Labels labels) {
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

    public void merge(GaugeMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            value = metric.getValue();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
