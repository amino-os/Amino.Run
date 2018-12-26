package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Collector;
import sapphire.sysSapphireObjects.metricCollector.MetricSchema;

public class Schema implements MetricSchema {
    private String metricName;
    private Labels mandatoryLabels;

    public Schema(String metricName, Labels labels) {
        this.mandatoryLabels = labels;
        this.metricName = metricName;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Collector getCollector() {
        return new CounterCollector(mandatoryLabels);
    }
}
