package amino.run.sysSapphireObjects.metricCollector.metric.gauge;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.Collector;
import amino.run.sysSapphireObjects.metricCollector.MetricSchema;

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
