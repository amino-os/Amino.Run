package amino.run.app;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

/**
 * Specification for node selection. Users use {@code NodeSelectorSpec} to specify on which nodes
 * (i.e. kernel servers) to run their sapphire object.
 *
 * <p>At present we only support specifying {@code NodeSelectorSpec} at sapphire object level. We
 * will consider support specifying {@code NodeSelectorSpec} at DM level in the future if necessary.
 *
 * <p>{@code NodeSelectorSpec} contains two label sets, a {@code orLabels} set and a {@code
 * andLabels} set. {@code orLabels} set and {@code andLabels} set are considered as selector used to
 * select nodes.
 *
 * <p>If {@code orLabels} set is not empty, then a node will be selected only if it contains any
 * label specified in {@code orLabels} set. If {@code andLabels} set is not empty, then a node will
 * be selected only if it contains all labels specified in {@code andLabels} set. If both {@code
 * orLabels} and {@code andLabels} are specified, then a node will be selected only if it contains
 * all labels in {@code andLabels} set <strong>and</strong> some label in {@code orLabels} set.
 *
 * <p>By default, both {@code orLabels} set and {@code andLabels} set are empty which means no
 * selector will be applied in which case all nodes will be returned.
 */
public class NodeSelectorSpec implements Serializable {
    public Set<String> orLabels = new HashSet<>();
    public Set<String> andLabels = new HashSet<>();
    public boolean topologicalAffinity = true;

    public Set<String> getOrLabels() {
        return Collections.unmodifiableSet(orLabels);
    }

    public Set<String> getAndLabels() {
        return Collections.unmodifiableSet(andLabels);
    }

    public boolean isTopologicalAffinity() {
        return topologicalAffinity;
    }

    public void enableTopologicalAffinity() {
        topologicalAffinity = true;
    }
    /**
     * Adds the label into {@code andLabels} set Null label or empty label is ignored.
     *
     * @param label a label
     */
    public void addAndLabel(String label) {
        if (label == null || label.isEmpty()) {
            return;
        }
        this.andLabels.add(label);
    }

    /**
     * Adds the given label set into the {@code andLabels} set Null label set is ignored.
     *
     * @param andLabels a label set
     */
    public void addAndLabels(Set<String> andLabels) {
        if (andLabels == null) {
            return;
        }
        this.andLabels.addAll(andLabels);
    }

    /**
     * Adds the label into {@code orLabels} set. Null label or empty label is ignored.
     *
     * @param label a label
     */
    public void addOrLabel(String label) {
        if (label == null || label.isEmpty()) {
            return;
        }

        this.orLabels.add(label);
    }

    /**
     * Adds the given label set into the {@code orLabels} set Null label set is ignored.
     *
     * @param orLabels a label set
     */
    public void addOrLabels(Set<String> orLabels) {
        if (orLabels == null) {
            return;
        }

        this.orLabels.addAll(orLabels);
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
        NodeSelectorSpec that = (NodeSelectorSpec) o;
        return Objects.equals(orLabels, that.orLabels) && Objects.equals(andLabels, that.andLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orLabels, andLabels);
    }
}
