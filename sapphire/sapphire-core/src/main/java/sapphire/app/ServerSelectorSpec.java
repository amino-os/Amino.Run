package sapphire.app;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;

public class ServerSelectorSpec implements Serializable {
    private Map<String, String> labels = new HashMap<>();

    public void addLabel(String key, String val) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid blank key '%s'", key));
        }
        if (val == null || val.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid blank value '%s'", val));
        }
        this.labels.put(key, val);
    }

    public void addLabels(Map<String, String> labelMap) {
        if (labelMap == null) {
            throw new NullPointerException("labelMap is null");
        }
        this.labels.putAll(labelMap);
    }

    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerSelectorSpec that = (ServerSelectorSpec) o;
        return Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labels);
    }
}
