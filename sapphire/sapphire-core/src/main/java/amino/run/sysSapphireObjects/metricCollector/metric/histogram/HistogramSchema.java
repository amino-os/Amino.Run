package amino.run.sysSapphireObjects.metricCollector.metric.histogram;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.Collector;
import amino.run.sysSapphireObjects.metricCollector.MetricSchema;

public class HistogramSchema implements MetricSchema {
    private String metricName;
    private Labels mandatoryLabels;

    public HistogramSchema(String metricName, Labels labels) {
        this.mandatoryLabels = labels;
        this.metricName = metricName;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Collector getCollector() {
        return new HistogramCollector(mandatoryLabels);
    }
}
