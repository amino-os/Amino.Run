package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import sapphire.app.labelselector.Selector;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;

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
