package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import sapphire.app.labelselector.Selector;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;

public class GaugeMetricSelector implements MetricSelector {
    private Selector selector;
    private String metricName;

    public GaugeMetricSelector(String name, Selector selector) {
        this.selector = selector;
        this.metricName = name;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetricSelector() {
        return this;
    }

    public Selector getSelector() {
        return selector;
    }
}
