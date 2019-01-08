package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import amino.run.sysSapphireObjects.metricCollector.MetricWithSelector;
import java.util.logging.Logger;

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
        synchronized (this) {
            count = count + metric.getCount();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
