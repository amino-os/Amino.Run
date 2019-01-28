package amino.run.policy.metric;

import amino.run.app.MicroServiceSpec;
import amino.run.app.labelselector.Labels;
import amino.run.policy.DefaultPolicy;
import java.util.ArrayList;
import java.util.Objects;

public class MetricPolicy extends DefaultPolicy {
    // TODO define default labels for Metric collector
    private static final long DM_METRIC_UPDATE_FREQUENCY = 3000; // milliseconds

    /** Configuration for Metric Policy. */
    public static class Config implements SapphirePolicyConfig {
        private Labels metricLabels = Labels.newBuilder().create();
        private long metricUpdateFrequency = DM_METRIC_UPDATE_FREQUENCY;
        private boolean migrationEnabled = false;

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

        public void setMigrationEnabled(boolean enable) {
            migrationEnabled = enable;
        }

        public boolean isMigrationEnabled() {
            return migrationEnabled;
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
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            super.onCreate(group, spec);
            aggregator = new MetricAggregator(spec, group.getSapphireObjId(), getReplicaId());
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (!aggregator.isInitialized()) {
                aggregator.initialize();
            }

            aggregator.incRpcCounter();

            return aggregator.executionTime(() -> super.onRPC(method, params));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            aggregator.stop();
        }
    }

    public static class MetricGroupPolicy extends DefaultGroupPolicy {}
}
