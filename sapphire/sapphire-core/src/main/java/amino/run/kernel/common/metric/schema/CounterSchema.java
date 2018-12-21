package amino.run.kernel.common.metric.schema;

import java.util.HashMap;

public class CounterSchema extends Schema {
    public CounterSchema(String metricName, HashMap<String, String> labels) {
        super(metricName, labels);
    }

    @Override
    public String getMetricType() {
        return "CounterMetric";
    }
}
