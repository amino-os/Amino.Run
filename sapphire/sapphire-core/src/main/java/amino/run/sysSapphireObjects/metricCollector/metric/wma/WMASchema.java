package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.Collector;
import amino.run.sysSapphireObjects.metricCollector.MetricSchema;

public class WMASchema implements MetricSchema {
    private String metricName;
    private Labels mandatoryLabels;
    private int bucketSize;

    public WMASchema(String metricName, Labels labels, int bucketSize) {
        this.mandatoryLabels = labels;
        this.metricName = metricName;
        this.bucketSize = bucketSize;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Collector getCollector() {
        return new WMACollector(mandatoryLabels, bucketSize);
    }
}
