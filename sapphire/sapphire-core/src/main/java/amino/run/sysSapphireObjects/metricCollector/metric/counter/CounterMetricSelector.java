package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;

public class CounterMetricSelector implements MetricSelector {
    private Selector selector;
    private String metricName;

    public String getName() {
        return this.metricName;
    }

    public Object getMetricSelector() {
        return this;
    }

    public Selector getSelector() {
        return this.selector;
    }

    public CounterMetricSelector(String name, Selector selector) {
        this.selector = selector;
        this.metricName = name;
    }

    @Override
    public String toString() {
        return "CounterMetricSelector < metricName: "
                + metricName
                + ","
                + "Selector: "
                + selector
                + ">";
    }
}
