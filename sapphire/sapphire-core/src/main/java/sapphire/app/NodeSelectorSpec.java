package sapphire.app;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import sapphire.common.LabelUtils;

/**
 * Specification for node selection. Users use {@code NodeSelectorSpec} to specify on which nodes
 * (i.e. kernel servers) to run their sapphire object.
 *
 * <p>At present we only support specifying {@code NodeSelectorSpec} at sapphire object level. We
 * will consider support specifying {@code NodeSelectorSpec} at DM level in the future if necessary.
 *
 * <p>{@code NodeSelectorSpec} contains matchLabels of type map, a {@code matchLabels} map and a
 * {@code nodeAffinity} NodeAffinity. {@code matchLabels} map and {@code nodeAffinity} NodeAffinity
 * are considered as selector used to select nodes.
 *
 * <p>If {@code matchLabels} map is not empty, then a node will be selected only if it contains all
 * label specified in {@code matchLabels} map.
 *
 * <p>By default, both {@code matchLabels} map and {@code nodeAffinity} NodeAffinity are empty which
 * means no selector will be applied in which case all nodes will be returned.
 */
public class NodeSelectorSpec implements Serializable {

    private Map<String, String> matchLabels = null;
    private NodeAffinity nodeAffinity = null;

    private static Logger logger = Logger.getLogger(NodeSelectorSpec.class.getName());

    public Map<String, String> getMatchLabels() {
        return matchLabels;
    }

    public void setMatchLabels(Map<String, String> matchLabels) throws IllegalArgumentException {
        if (matchLabels == null || matchLabels.isEmpty()) {
            this.matchLabels = matchLabels;
            return;
        }
        Set<Map.Entry<String, String>> entries = matchLabels.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (!LabelUtils.validateLabelKey(entry.getKey())
                    || !LabelUtils.validateLabelValue(entry.getValue())) {
                logger.warning("validateLabelValue /validateLabelValue failed ");
                throw new IllegalArgumentException(
                        "invalid matchLabels are not allowed" + matchLabels);
            }
        }
        this.matchLabels = matchLabels;
    }

    public void addMatchLabelsItem(String key, String value) throws IllegalArgumentException {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            logger.warning("null or empty key/value are not allowed");
            throw new IllegalArgumentException(
                    "null or empty key/value are not allowed key:" + key + "value:" + value);
        }
        if (!LabelUtils.validateLabelKey(key) || !LabelUtils.validateLabelValue(value)) {
            logger.warning("validateLabelValue /validateLabelValue failed ");
            throw new IllegalArgumentException(
                    "Invalid input Argument are not allowed key:" + key + "value:" + value);
        }

        if (this.matchLabels == null) {
            this.matchLabels = new HashMap<String, String>();
        }
        this.matchLabels.put(key, value);
        return;
    }

    public NodeAffinity getNodeAffinity() {
        return nodeAffinity;
    }

    public void setNodeAffinity(NodeAffinity nodeAffinity) throws IllegalArgumentException {
        if (!LabelUtils.validateNodeAffinity(nodeAffinity)) {
            logger.warning("null or empty nodeAffinity or invalid nodeAffinity are not allowed");
            throw new IllegalArgumentException(
                    "Invalid input Argument not allowed " + nodeAffinity);
        }

        this.nodeAffinity = nodeAffinity;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeSelectorSpec specSelector = (NodeSelectorSpec) o;
        return Objects.equals(this.nodeAffinity, specSelector.nodeAffinity)
                && Objects.equals(this.matchLabels, specSelector.matchLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeAffinity, matchLabels);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }
}
