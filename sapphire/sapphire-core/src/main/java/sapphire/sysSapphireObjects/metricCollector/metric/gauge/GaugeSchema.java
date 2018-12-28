package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Collector;
import sapphire.sysSapphireObjects.metricCollector.MetricSchema;

public class GaugeSchema implements MetricSchema {
    private String metricName;
    private Labels mandatoryLabels;

    public GaugeSchema(String metricName, Labels labels) {
        this.mandatoryLabels = labels;
        this.metricName = metricName;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Collector getCollector() {
        return new GaugeCollector(mandatoryLabels);
    }
}
