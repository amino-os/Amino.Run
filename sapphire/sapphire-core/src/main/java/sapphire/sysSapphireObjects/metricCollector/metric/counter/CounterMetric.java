package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class CounterMetric implements Metric {
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

    public CounterMetric(String name, Labels labels) {
        this.metricName = name;
        this.labels = labels;
    }

    public CounterMetric(String name, int count) {
        this.metricName = name;
        this.count = count;
    }

    public void reset() {
        count = 0;
    }

    public long getCount() {
        return count;
    }

    public synchronized void incCount() {
        count++;
    }

    public Labels getLabels() {
        return labels;
    }

    public void merge(CounterMetric metric) {
        count = count + metric.getCount();
    }
}
