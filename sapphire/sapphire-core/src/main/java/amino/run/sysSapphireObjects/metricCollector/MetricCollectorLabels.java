package amino.run.sysSapphireObjects.metricCollector;

import amino.run.app.labelselector.Labels;

public class MetricCollectorLabels {
    public static final Labels labels;

    static {
        labels = Labels.newBuilder().add("sys/sapphireObject", "MetricCollector").create();
    }
}
