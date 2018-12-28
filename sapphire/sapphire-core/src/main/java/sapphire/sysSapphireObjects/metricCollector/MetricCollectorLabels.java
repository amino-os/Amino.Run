package sapphire.sysSapphireObjects.metricCollector;

import java.util.HashMap;
import java.util.Map;
import sapphire.app.labelselector.Labels;

public class MetricCollectorLabels {
    public static final Labels labels;

    static {
        Map<String, String> labelsMap = new HashMap<String, String>();
        labelsMap.put("sys/sapphireObject", "MetricCollector");

        labels = new Labels();
        labels.setLabels(labelsMap);
    }
}
