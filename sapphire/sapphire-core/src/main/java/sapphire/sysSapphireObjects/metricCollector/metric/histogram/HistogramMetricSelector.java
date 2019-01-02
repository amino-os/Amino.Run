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
