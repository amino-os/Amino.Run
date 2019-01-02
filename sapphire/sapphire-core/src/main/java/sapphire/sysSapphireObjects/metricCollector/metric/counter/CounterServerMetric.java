package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class CounterServerMetric implements Metric {
    private static Logger logger = Logger.getLogger(CounterClientMetric.class.getName());
    private String metricName;
    private long count;
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
        return metricName + "<" + labels.toString() + ":" + count + ">";
    }

    public CounterServerMetric(String name, Labels labels) {
        this.metricName = name;
        this.labels = labels;
    }

    public void merge(
            sapphire.sysSapphireObjects.metricCollector.metric.counter.CounterClientMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            count = count + metric.getCount();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
