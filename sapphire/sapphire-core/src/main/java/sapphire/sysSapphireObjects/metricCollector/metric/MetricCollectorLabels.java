package sapphire.sysSapphireObjects.metricCollector.metric;

import sapphire.app.labelselector.Labels;

import java.util.HashMap;
import java.util.Map;

public class MetricCollectorLabels {
    public static final Labels labels;

    static {
        Map<String, String> labelsMap = new HashMap<String, String>();
        labelsMap.put("sys/sapphireObject", "MetricCollector");

        labels = new Labels();
        labels.setLabels(labelsMap);
    }
}
