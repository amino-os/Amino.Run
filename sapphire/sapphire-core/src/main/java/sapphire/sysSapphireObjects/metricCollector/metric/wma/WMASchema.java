package sapphire.sysSapphireObjects.metricCollector.metric.wma;

import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Collector;
import sapphire.sysSapphireObjects.metricCollector.MetricSchema;

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
