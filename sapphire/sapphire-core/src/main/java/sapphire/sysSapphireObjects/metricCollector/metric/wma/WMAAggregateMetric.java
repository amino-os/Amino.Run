package sapphire.sysSapphireObjects.metricCollector.metric.wma;

import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class WMAAggregateMetric implements Metric {
    private static Logger logger = Logger.getLogger(WMAAggregateMetric.class.getName());

    private String metricName;
    private float value;
    private Labels labels;

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + value + ">";
    }

    public WMAAggregateMetric(String name, Labels labels) {
        this.metricName = name;
        this.labels = labels;
    }

    public Labels getLabels() {
        return labels;
    }

    public void merge(WMAMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            value = metric.getValue();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
