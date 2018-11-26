package sapphire.app;

import java.io.Serializable;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

/**
 * Specification for node selection. Users use {@code NodeSelectorSpec} to specify on which nodes
 * (i.e. kernel servers) to run their sapphire object.
 *
 * <p>At present we only support specifying {@code NodeSelectorSpec} at sapphire object level. We
 * will consider support specifying {@code NodeSelectorSpec} at DM level in the future if necessary.
 *
 * <p>{@code NodeSelectorSpec} contains two label maps, a {@code orLabels} map and a {@code
 * andLabels} map. {@code orLabels} map and {@code andLabels} map are considered as selector used to
 * select nodes.
 *
 * <p>If {@code orLabels} map is not empty, then a node will be selected only if it contains any
 * label specified in {@code orLabels} map. If {@code andLabels} map is not empty, then a node will
 * be selected only if it contains all labels specified in {@code andLabels} map. If both {@code
 * orLabels} and {@code andLabels} are specified, then a node will be selected only if it contains
 * all labels in {@code andLabels} map <strong>and</strong> some label in {@code orLabels} map.
 *
 * <p>By default, both {@code orLabels} map and {@code andLabels} map are empty which means no
 * selector will be applied in which case all nodes will be returned.
 */
public class NodeSelectorSpec implements Serializable {
    public HashMap orLabels = new HashMap();
    public HashMap andLabels = new HashMap();

    public HashMap getOrLabels() {
        return orLabels;
    }

    public HashMap getAndLabels() {
        return andLabels;
    }

    /**
     * Adds the key & value into {@code andLabels} map Null key/value or empty key/value is ignored.
     *
     * @param key
     * @param value
     */
    public void addAndLabel(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return;
        }
        this.andLabels.put(key, value);
    }

    /**
     * Adds the given key & value into the {@code andLabels} map Null key/value is ignored.
     *
     * @param keyValues
     */
    public void addAndLabels(HashMap keyValues) {
        if (keyValues == null) {
            return;
        }
        this.andLabels.putAll(keyValues);
    }

    /**
     * Adds the label into {@code orLabels} map. Null key/value or empty key/value is ignored.
     *
     * @param key
     * @param value
     */
    public void addOrLabel(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return;
        }

        this.orLabels.put(key, value);
    }

    /**
     * Adds the given label map into the {@code orLabels} map Null keyValues map is ignored.
     *
     * @param keyValues
     */
    public void addOrLabels(HashMap keyValues) {
        if (keyValues == null) {
            return;
        }

        this.orLabels.putAll(keyValues);
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
