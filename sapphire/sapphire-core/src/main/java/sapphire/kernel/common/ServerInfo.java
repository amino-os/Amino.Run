package sapphire.kernel.common;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import sapphire.app.KsAffinity;
import sapphire.app.NodeSelectorRequirement;
import sapphire.app.NodeSelectorTerm;
import sapphire.common.LabelUtils;

/** {@code ServerInfo} contains meta data of a kernel server. */
public class ServerInfo implements Serializable {
    private InetSocketAddress host;

    private Map<String, String> labels = new HashMap<String, String>();
    private static Logger logger = Logger.getLogger(ServerInfo.class.getName());

    public ServerInfo(InetSocketAddress addr) {
        this.host = addr;
    }

    public InetSocketAddress getHost() {
        return host;
    }

    public String getRegion() {
        return labels.get(LabelUtils.REGION_KEY);
    }

    public void addLabels(Map keyValues) {
        if (keyValues == null) {
            throw new NullPointerException("Labels must not be null");
        }
        this.labels.putAll(keyValues);
    }

    /**
     * Checks if this server contains <strong>any</strong> label specified in the given label map.
     * If the specified label map is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param labels a map of labels
     * @return {@code true} if the server contains any label in the label map; {@code false}
     *     otherwise. Returns {@code true} if the given label map is {@code null} or empty.
     */
    public boolean containsAny(Map labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }
        Set<Map.Entry<String, String>> entries = labels.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            if (this.labels.containsKey(entry.getKey())
                    && this.labels.containsValue(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given label map.
     * If the specified label map is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param labels a map of labels
     * @return {@code true} if the server contains all labels in the label map; {@code false}
     *     otherwise. Returns {@code true} if the given label map is {@code null} or empty.
     */
    public boolean containsAll(Map labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }
        Set<Map.Entry<String, String>> entries = labels.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            if (!this.labels.containsKey(entry.getKey())
                    || !this.labels.containsValue(entry.getValue())) {
                logger.severe(
                        "containsAll return false as this server doesn't have the required labels: "
                                + entries.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given KsAffinity.
     * If the specified KsAffinity is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param affinity KsAffinity
     * @return {@code true} if the server matches the given affinity requirement; {@code false}
     *     otherwise. Returns {@code true} if the given affinity is {@code null}.
     */
    public boolean matchNodeAffinity(KsAffinity affinity) {
        if (affinity == null) {
            return true;
        }
        List<NodeSelectorTerm> terms = affinity.getRequireExpressions();
        if ((terms == null) || (terms.size() == 0)) {
            return true;
        }
        for (int i = 0; i < terms.size(); i++) {
            NodeSelectorTerm term = terms.get(i);
            List<NodeSelectorRequirement> MatchExpressions = term.getMatchExpressions();
            List<NodeSelectorRequirement> MatchFields = term.getMatchFields();

            // nil or empty term selects no objects
            if (MatchExpressions == null && MatchFields == null) {
                continue;
            }
            if ((MatchExpressions != null && MatchExpressions.size() == 0)
                    && (MatchFields != null && MatchFields.size() == 0)) {
                continue;
            }
            if (MatchExpressions != null && MatchExpressions.size() != 0) {
                if (!matchExpressions(MatchExpressions)) {
                    continue;
                }
            }
            if (MatchFields != null && MatchFields.size() != 0) {
                if (!matchFileds(MatchFields)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given label map.
     * If the specified label map is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param matchExpItem NodeSelectorRequirement
     * @return {@code true} if the server matches matchExpItem; {@code false} otherwise.
     */

    // There is a match in the following cases:
    // (1) The operator is Exists and Labels has the Requirement's key.
    // (2) The operator is In, Labels has the Requirement's key and Labels'
    //     value for that key is in Requirement's value set.
    // (3) The operator is NotIn, Labels has the Requirement's key and
    //     Labels' value for that key is not in Requirement's value set.
    // (4) The operator is DoesNotExist or NotIn and Labels does not have the
    //     Requirement's key.
    // (5) The operator is GreaterThanOperator or LessThanOperator, and Labels has
    //     the Requirement's key and the corresponding value satisfies mathematical inequality.

    public boolean matcheLabelSelector(NodeSelectorRequirement matchExpItem) {

        switch (matchExpItem.operator) {
            case LabelUtils.In:
            case LabelUtils.Equals:
            case LabelUtils.DoubleEquals:
                if (!this.labels.containsKey(matchExpItem.key)) {
                    return false;
                }
                return matchExpItem.values.contains(this.labels.get(matchExpItem.key));
            case LabelUtils.NotIn:
            case LabelUtils.NotEquals:
                if (!this.labels.containsKey(matchExpItem.key)) {
                    logger.warning("keys doesn't match");
                    return true;
                }
                return !matchExpItem.values.contains(this.labels.get(matchExpItem.key));
            case LabelUtils.Exists:
                return this.labels.containsKey(matchExpItem.key);
            case LabelUtils.DoesNotExist:
                return !this.labels.containsKey(matchExpItem.key);
            case LabelUtils.GreaterThan:
            case LabelUtils.LessThan:
                if (!this.labels.containsKey(matchExpItem.key)) {
                    return false;
                }
                // There should be only one strValue in r.strValues, and can be converted to a
                // integer.
                if (matchExpItem.values.size() != 1) {
                    logger.severe(
                            "Invalid values count %+v of requirement %#v, for 'Gt', 'Lt' operators, exactly one value is required"
                                    + matchExpItem.values.size()
                                    + matchExpItem);
                    return false;
                }
                long lsValue, rValue;
                try {
                    lsValue = Integer.parseInt(this.labels.get(matchExpItem.key));
                    rValue = Integer.parseInt(matchExpItem.values.get(0));

                } catch (NumberFormatException e) {
                    logger.severe("for 'Gt', 'Lt' operators,labels value is not valid");
                    return false;
                }
                return ((matchExpItem.operator == LabelUtils.GreaterThan && lsValue > rValue)
                        || (matchExpItem.operator == LabelUtils.LessThan && lsValue < rValue));
            default:
                return false;
        }
    }

    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given label map.
     * If the specified label map is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param matchExprs a list of NodeSelectorRequirement
     * @return {@code true} if the server matches all matchExprs in the list; {@code false}
     *     otherwise. Returns {@code true} if the given matchExprs is {@code null} or empty.
     */
    // returns true if all its Requirements match the input Labels.
    // If any Requirement does not match, false is returned.
    public boolean matchExpressions(List<NodeSelectorRequirement> matchExprs) {
        if ((matchExprs == null) || (matchExprs.size() == 0)) {
            return true;
        }
        for (int i = 0; i < matchExprs.size(); i++) {
            NodeSelectorRequirement matchExpItem = matchExprs.get(i);
            if (!matcheLabelSelector(matchExpItem)) {
                logger.severe("matcheLabelSelector  failed for matchExpItem:" + matchExpItem);
                return false;
            }
        }
        return true;
    }

    // TODO here we should consider the Node Fields as per  k8s, currently just implemented based on
    // the Node/KS labels
    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given label map.
     * If the specified label map is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param matchFieldItem NodeSelectorRequirement
     * @return {@code true} if the server matches matchFieldItem; {@code false} otherwise.
     */
    public boolean matcheFieldSelector(NodeSelectorRequirement matchFieldItem) {
        if (matchFieldItem.values.size() > 1) {
            logger.severe("matcheFieldSelector  only single value is allowed:" + matchFieldItem);
            return false;
        }
        switch (matchFieldItem.operator) {
            case LabelUtils.In:
                return matchFieldItem.values.get(0).equals(this.labels.get(matchFieldItem.key));
            case LabelUtils.NotIn:
                return !matchFieldItem.values.get(0).equals(this.labels.get(matchFieldItem.key));
            default:
                return false;
        }
    }

    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given label map.
     * If the specified label map is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param matchFields a list of NodeSelectorRequirement
     * @return {@code true} if the server matches all matchExprs in the list; {@code false}
     *     otherwise. Returns {@code true} if the given matchExprs is {@code null} or empty.
     */
    // returns true if all its Requirements match the Labels.
    // If any Requirement does not match, false is returned.
    public boolean matchFileds(List<NodeSelectorRequirement> matchFields) {
        if ((matchFields == null) || (matchFields.size() == 0)) {
            return true;
        }
        for (int i = 0; i < matchFields.size(); i++) {
            NodeSelectorRequirement matchFieldItem = matchFields.get(i);
            if (!matcheFieldSelector(matchFieldItem)) {
                logger.severe("matcheFieldSelector  failed for matchFieldItem:" + matchFieldItem);
                return false;
            }
        }
        return true;
    }
}
