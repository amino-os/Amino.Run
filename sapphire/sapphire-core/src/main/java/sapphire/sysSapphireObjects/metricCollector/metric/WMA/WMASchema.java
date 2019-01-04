package sapphire.sysSapphireObjects.metricCollector.metric.WMA;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Collector;
import sapphire.sysSapphireObjects.metricCollector.MetricSchema;

public class WMASchema implements MetricSchema {
    private String metricName;
    private Labels mandatoryLabels;

    public WMASchema(String metricName, Labels labels) {
        this.mandatoryLabels = labels;
        this.metricName = metricName;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Collector getCollector() {
        return new WMACollector(mandatoryLabels);
    }
}
