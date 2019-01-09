package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;
import sapphire.sysSapphireObjects.metricCollector.MetricWithSelector;

public class CounterMetricAggregator implements MetricWithSelector {
    private static Logger logger = Logger.getLogger(CounterMetricAggregator.class.getName());
    private String metricName;
    private long count;
    private Labels labels;

    @Override
    public Metric getMetric(MetricSelector metricSelector) {
        CounterMetric counterMetric =
                CounterMetric.newBuilder()
                        .setMetricName(metricName)
                        .setLabels(labels)
                        .setCount(count)
                        .create();
        return counterMetric;
    }

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + count + ">";
    }

    public CounterMetricAggregator(String name, Labels labels) {
        this.metricName = name;
        this.labels = labels;
    }

    public void merge(CounterMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            count = count + metric.getCount();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
