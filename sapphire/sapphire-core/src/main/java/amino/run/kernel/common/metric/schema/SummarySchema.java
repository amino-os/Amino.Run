package amino.run.kernel.common.metric.schema;

import java.util.HashMap;

public class SummarySchema extends Schema {
    public SummarySchema(String metricName, HashMap<String, String> labels) {
        super(metricName, labels);
    }

    @Override
    public String getMetricType() {
        return "SummarySchema";
    }
}
