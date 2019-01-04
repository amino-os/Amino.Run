package sapphire.sysSapphireObjects.metricCollector.metric.WMA;

import sapphire.app.labelselector.Selector;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;

public class WMAMetricSelector implements MetricSelector {
    private Selector selector;
    private String metricName;

    public WMAMetricSelector(String name, Selector selector) {
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
