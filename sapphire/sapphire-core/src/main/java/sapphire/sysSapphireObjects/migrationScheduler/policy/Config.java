package sapphire.sysSapphireObjects.migrationScheduler.policy;

import static sapphire.policy.mobility.automigration.MetricDMConstants.AUTO_MIGRATION_AVG_EXECUTION_TIME;

import java.io.Serializable;
import org.yaml.snakeyaml.Yaml;
import sapphire.app.labelselector.Labels;

public class Config implements Serializable {
    private long metricCollectFrequency = 6000; // milliseconds
    private String metricName = AUTO_MIGRATION_AVG_EXECUTION_TIME;
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
