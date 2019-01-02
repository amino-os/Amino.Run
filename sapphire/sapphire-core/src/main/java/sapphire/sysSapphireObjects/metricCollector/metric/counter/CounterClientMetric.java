package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricCollector;

public class CounterClientMetric implements Metric {
    private static Logger logger = Logger.getLogger(CounterClientMetric.class.getName());

    private String metricName;
    private long count;
    private Labels labels;
    private MetricCollector collectorStub;

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

    public CounterClientMetric(String name, Labels labels, MetricCollector metricCollector) {
        this.metricName = name;
        this.labels = labels;
        this.collectorStub = metricCollector;
    }

    public CounterClientMetric(String name, int count) {
        this.metricName = name;
        this.count = count;
    }

    public void reset() {
        synchronized (this) {
            count = 0;
        }
    }

    public boolean modified() {
        return count != 0;
    }

    public long getCount() {
        return count;
    }

    public void incCount() {
        synchronized (this) {
            count++;
        }
    }

    public Labels getLabels() {
        return labels;
    }

    public void sendMetric() throws Exception {
        collectorStub.push(this);
    }

    public static CounterClientMetric.Builder newBuilder() {
        return new CounterClientMetric.Builder();
    }

    public static class Builder {
        private String metricName;
        private Labels labels;
        private MetricCollector metricCollector;

        public CounterClientMetric.Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public CounterClientMetric.Builder setLabels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public CounterClientMetric.Builder setMetricCollector(MetricCollector metricCollector) {
            this.metricCollector = metricCollector;
            return this;
        }

        public CounterClientMetric create() {
            return new CounterClientMetric(metricName, labels, metricCollector);
        }
    }
}
