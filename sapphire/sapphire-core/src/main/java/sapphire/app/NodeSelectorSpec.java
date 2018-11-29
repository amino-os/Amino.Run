package sapphire.app;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import sapphire.common.Utils;

/**
 * Specification for node selection. Users use {@code NodeSelectorSpec} to specify on which nodes
 * (i.e. kernel servers) to run their sapphire object.
 *
 * <p>At present we only support specifying {@code NodeSelectorSpec} at sapphire object level. We
 * will consider support specifying {@code NodeSelectorSpec} at DM level in the future if necessary.
 *
 * <p>{@code NodeSelectorSpec} contains matchLabels of type map, a {@code matchLabels} map and a
 * {@code matchExpressions} NodeSelectorRequirement. {@code matchLabels} map and {@code
 * matchExpressions} NodeSelectorRequirement are considered as selector used to select nodes.
 * currently matchLabels only used in future we will use the matchExpressions (list of
 * NodeSelectorRequirement)
 *
 * <p>If {@code matchLabels} map is not empty, then a node will be selected only if it contains all
 * label specified in {@code matchLabels} map.
 *
 * <p>By default, both {@code matchLabels} map and {@code matchExpressions} list of
 * NodeSelectorRequirement are empty which means no selector will be applied in which case all nodes
 * will be returned.
 */
public class NodeSelectorSpec implements Serializable {

    private Map<String, String> matchLabels = null;
    private List<NodeSelectorRequirement> matchExpressions = null;
    private static Logger logger = Logger.getLogger(NodeSelectorSpec.class.getName());

    public Map<String, String> getMatchLabels() {
        return matchLabels;
    }

    public void setMatchLabels(Map<String, String> matchLabels) {
        if (matchLabels == null || matchLabels.isEmpty()) {
            logger.warning("null or empty matchLabels are not allowed");
            return;
        }
        Set<Map.Entry<String, String>> entries = matchLabels.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (!Utils.validateLabelKey(entry.getKey())
                    || !Utils.validateLabelValue(entry.getValue())) {
                logger.warning("validateLabelValue /validateLabelValue failed ");
                return;
            }
        }
        this.matchLabels = matchLabels;
    }

    public void addMatchLabelsItem(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            logger.warning("null or empty key/value are not allowed");
            return;
        }
        if (!Utils.validateLabelKey(key) || !Utils.validateLabelValue(value)) {
            logger.warning("validateLabelValue /validateLabelValue failed ");
            return;
        }

        if (this.matchLabels == null) {
            this.matchLabels = new HashMap<String, String>();
        }
        this.matchLabels.put(key, value);
        return;
    }

    public List<NodeSelectorRequirement> getMatchExpressions() {
        return matchExpressions;
    }

    public void setMatchExpressions(List<NodeSelectorRequirement> matchExpressions) {
        if (matchExpressions == null || matchExpressions.isEmpty()) {
            logger.warning("null or empty matchExpressions are not allowed");
            return;
        }

        for (int i = 0; i < matchExpressions.size(); i++) {
            NodeSelectorRequirement matchExpressionsItem = matchExpressions.get(i);
            if (!Utils.validateNodeSelectRequirement(
                    matchExpressionsItem.key,
                    matchExpressionsItem.operator,
                    matchExpressionsItem.values)) {
                logger.warning("validateNodeSelectRequirement failed");
                return;
            }
        }

        this.matchExpressions = matchExpressions;
    }

    public void addMatchExpressionsItem(NodeSelectorRequirement matchExpressionsItem) {
        if (matchExpressionsItem == null) {
            return;
        }
        if (!Utils.validateNodeSelectRequirement(
                matchExpressionsItem.key,
                matchExpressionsItem.operator,
                matchExpressionsItem.values)) {
            logger.warning("validateNodeSelectRequirement failed");
            return;
        }

        if (this.matchExpressions == null) {
            this.matchExpressions = new ArrayList<NodeSelectorRequirement>();
        }
        this.matchExpressions.add(matchExpressionsItem);
        return;
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
        return Objects.equals(this.matchExpressions, specSelector.matchExpressions)
                && Objects.equals(this.matchLabels, specSelector.matchLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchExpressions, matchLabels);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }
}
