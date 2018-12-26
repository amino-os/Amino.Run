package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import sapphire.app.labelselector.Selector;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;

public class CounterMetricSelector implements MetricSelector {
    private Selector selector;
    private String metricName;

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

    public CounterMetricSelector(String name, Selector selector) {
        this.selector = selector;
        this.metricName = name;
    }
}
