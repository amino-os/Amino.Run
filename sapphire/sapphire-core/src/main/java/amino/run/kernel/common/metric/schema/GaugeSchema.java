package amino.run.kernel.common.metric.schema;

import java.util.HashMap;

public class GaugeSchema extends Schema {
    public GaugeSchema(String metricName, HashMap<String, String> labels) {
        super(metricName, labels);
    }

    @Override
    public String getMetricType() {
        return "GaugeMetric";
    }
}
