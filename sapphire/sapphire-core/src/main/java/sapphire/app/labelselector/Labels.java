package sapphire.app.labelselector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Labels are key-value pair used for tagging objects such as Sapphire Object for identity purpose.
 *
 * <p>Each Label maintains list of key value.Labels are immutable objects.
 */
public class Labels implements Serializable {
    private Map<String, String> labels = new HashMap<>();

    private Labels() {}

    public int size() {
        return labels.size();
    }

    public boolean has(String label) {
        return labels.containsKey(label);
    }

    public String get(String label) {
        return labels.get(label);
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Selector asSelector() {
        Selector selector = new Selector();
        Requirement req;

        for (Map.Entry label : labels.entrySet()) {
            req =
                    Requirement.newBuilder()
                            .key(label.getKey().toString())
                            .equal()
                            .value(label.getValue().toString())
                            .create();
            selector.add(req);
        }

        return selector;
    }

    public static Labels.Builder newBuilder() {
        return new Labels.Builder();
    }

    public static class Builder {
        private Map<String, String> labelMap = new HashMap<>();

        public Labels.Builder add(String key, String value) {
            labelMap.put(key, value);
            return this;
        }

        public Labels.Builder merge(Labels labels) {
            for (Map.Entry<String, String> entry : labels.getLabels().entrySet()) {
                labelMap.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Labels create() {
            Labels labels = new Labels();
            labels.setLabels(labelMap);
            return labels;
        }
    }

    @Override
    public String toString() {
        List<String> selectors = new ArrayList<>();
        for (Map.Entry label : labels.entrySet()) {
            selectors.add(label.getKey() + "=" + label.getValue());
        }
        return String.join(",", selectors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }
}
