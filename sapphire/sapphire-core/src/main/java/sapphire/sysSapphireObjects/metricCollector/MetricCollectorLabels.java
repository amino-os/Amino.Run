package sapphire.sysSapphireObjects.metricCollector;

import sapphire.app.labelselector.Labels;

public class MetricCollectorLabels {
    public static final Labels labels;

    static {
        labels = Labels.newBuilder().add("sys/sapphireObject", "MetricCollector").create();
    }
}
