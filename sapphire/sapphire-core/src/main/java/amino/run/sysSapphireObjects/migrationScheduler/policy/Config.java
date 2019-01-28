package amino.run.sysSapphireObjects.migrationScheduler.policy;

import static amino.run.policy.metric.MetricDMConstants.MIGRATION_AVG_EXECUTION_TIME;

import amino.run.app.labelselector.Labels;
import java.io.Serializable;
import org.yaml.snakeyaml.Yaml;

public class Config implements Serializable {
    private long metricCollectFrequency = 6000; // milliseconds
    private long migrationExecutionThreshold = 499999; // milliseconds
    private String metricName = MIGRATION_AVG_EXECUTION_TIME;
    private Labels metricLabels;

    public long getMetricCollectFrequency() {
        return metricCollectFrequency;
    }

    public void setMetricCollectFrequency(long metricCollectFrequency) {
        this.metricCollectFrequency = metricCollectFrequency;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Labels getMetricLabels() {
        return metricLabels;
    }

    public void setMetricLabels(Labels metricLabels) {
        this.metricLabels = metricLabels;
    }

    public void setMigrationExecutionThreshold(long migrationExecutionThreshold) {
        this.migrationExecutionThreshold = migrationExecutionThreshold;
    }

    public long getMigrationExecutionThreshold() {
        return migrationExecutionThreshold;
    }

    public static Config fromYaml(String yamlString) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(yamlString, Config.class);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }
}
