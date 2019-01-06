package sapphire.sysSapphireObjects.metricCollector.metric.wma;

import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class WMAMetric implements Metric {
    private static Logger logger = Logger.getLogger(WMAMetric.class.getName());

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

    public WMAMetric(String name, Labels labels) {
        this.metricName = name;
        this.labels = labels;
    }

    public boolean modified() {
        return value != 0;
    }

    public float getValue() {
        return value;
    }

    public void addValue() {
        synchronized (this) {
            value++;
        }
    }

    public Labels getLabels() {
        return labels;
    }
}
