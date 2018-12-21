package amino.run.app.labelselector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Labels are list of key-value pair used for objects tagging.
 *
 * <p>Labels objects are immutable and should only get created with {@link Labels.Builder}.
 * Application can use {@link Selector} to filter tagged objects.
 */
public class Labels implements Serializable {
    private Map<String, String> labels = new HashMap<>();

    private Labels() {}

    boolean has(String label) {
        return labels.containsKey(label);
    }

    // method is defined for Yaml parsing
    public String get(String label) {
        return labels.get(label);
    }

    // method is defined for Yaml parsing
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Create {@link Selector} with available labels.
     *
     * @return return {@link Selector} object
     */
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

    /**
     * Create {@link Labels.Builder} object for building Labels object
     *
     * @return {@link Labels.Builder} object
     */
    public static Labels.Builder newBuilder() {
        return new Labels.Builder();
    }

    public static class Builder {
        private Map<String, String> labelMap = new HashMap<>();

        /**
         * Append tags for Label
         *
         * @param key label's key
         * @param value label's value
         * @return Builder instance
         */
        public Labels.Builder add(String key, String value) {
            labelMap.put(key, value);
            return this;
        }

        /**
         * Merge key-value from {@link Labels} with available key-value pair
         *
         * @param labels
         * @return Builder instance
         */
        public Labels.Builder merge(Labels labels) {
            for (Map.Entry<String, String> entry : labels.getLabels().entrySet()) {
                labelMap.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Create {@link Labels} instance with available key-value pair
         *
         * @return Label instance
         */
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
