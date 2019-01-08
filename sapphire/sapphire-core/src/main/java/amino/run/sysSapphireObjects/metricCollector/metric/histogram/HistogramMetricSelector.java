package amino.run.sysSapphireObjects.metricCollector.metric.histogram;

import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;

public class HistogramMetricSelector implements MetricSelector {
    private Selector selector;
    private String metricName;

    public HistogramMetricSelector(String name, Selector selector) {
        this.selector = selector;
        this.metricName = name;
    }

    public String getName() {
        return this.metricName;
    }

    public Object getMetricSelector() {
        return this;
    }

    public Selector getSelector() {
        return this.selector;
    }
}
