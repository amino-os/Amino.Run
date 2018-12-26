package sapphire.app.labelselector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Labels implements Serializable {
    private Map<String, String> labels = new HashMap<>();

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
