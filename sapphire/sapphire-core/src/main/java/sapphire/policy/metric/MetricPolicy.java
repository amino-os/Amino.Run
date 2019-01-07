package sapphire.policy.metric;

import java.util.ArrayList;
import java.util.Objects;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.labelselector.Labels;
import sapphire.policy.DefaultSapphirePolicy;

public class MetricPolicy extends DefaultSapphirePolicy {
    // TODO define default labels for Metric collector
    private static final long DM_METRIC_UPDATE_FREQUENCY = 3000; // milliseconds

    /** Configuration for Metric Policy. */
    public static class Config implements SapphirePolicyConfig {
        private Labels metricLabels = Labels.newBuilder().create();
        private long metricUpdateFrequency = DM_METRIC_UPDATE_FREQUENCY;

        public long getMetricUpdateFrequency() {
            return metricUpdateFrequency;
        }

        public void setMetricUpdateFrequency(long frequency) {
            this.metricUpdateFrequency = frequency;
        }

        public Labels getMetricLabels() {
            return metricLabels;
        }

        public void setMetricLabels(Labels labels) {
            this.metricLabels = labels;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Config config = (Config) o;
            return metricLabels.toString().equals(config.metricLabels.toString());
        }

        @Override
        public int hashCode() {
            return Objects.hash(metricLabels);
        }
    }

    public static class MetricClientPolicy extends DefaultClientPolicy {}

    public static class MetricServerPolicy extends DefaultServerPolicy {
        private MetricAggregator aggregator;

        @Override
        public void onCreate(SapphireGroupPolicy group, SapphireObjectSpec spec) {
            super.onCreate(group, spec);
            aggregator = new MetricAggregator(spec, group.getSapphireObjId(), getReplicaId());
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (!aggregator.initialize()) {
                aggregator.$__initialize();
            }

            aggregator.incRpcCounter();

            return super.onRPC(method, params);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            aggregator.stop();
        }
    }

    public static class MetricGroupPolicy extends DefaultGroupPolicy {}
}
