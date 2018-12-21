package amino.run.kernel.common;

import amino.run.app.NodeSelectorSpec;
import amino.run.app.NodeSelectorTerm;
import amino.run.app.Requirement;
import amino.run.kernel.server.KernelServerImpl;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
        return labels.get(KernelServerImpl.REGION_KEY);
    }

    public void addLabels(Map keyValues) {
        if (keyValues == null) {
            throw new NullPointerException("Labels must not be null");
        }
        this.labels.putAll(keyValues);
    }

    /**
     * Checks if this server contains <strong>any</strong> label specified in the given label set.
     * If the specified label set is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param labels a set of labels
     * @return {@code true} if the server contains any label in the label set; {@code false}
     *     otherwise. Returns {@code true} if the given label set is {@code null} or empty.
     */
    public boolean containsAny(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (this.labels.containsKey(entry.getKey())
                    && this.labels.get(entry.getKey()).equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given label set.
     * If the specified label set is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param labels a set of labels
     * @return {@code true} if the server contains all labels in the label set; {@code false}
     *     otherwise. Returns {@code true} if the given label map is {@code null} or empty.
     */
    public boolean containsAll(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (!this.labels.containsKey(entry.getKey())
                    || !this.labels.get(entry.getKey()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public boolean matchNodeSelectorSpec(NodeSelectorSpec spec) {
        if (spec == null) {
            return true;
        }

        List<NodeSelectorTerm> terms = spec.getRequireExpressions();
        if ((terms == null) || (terms.size() == 0)) {
            return true;
        }

        for (NodeSelectorTerm term : terms) {
            List<Requirement> matchExpressions = term.getMatchExpressions();

            // nil or empty term selects no objects
            if (matchExpressions == null || matchExpressions.size() == 0) {
                continue;
            }

            if (!matchExpressions(matchExpressions)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean matchExpressions(List<Requirement> matchExprs) {
        if (matchExprs == null) {
            return true;
        }

        for (Requirement requirement : matchExprs) {
            if (!requirement.matches(labels)) {
                logger.severe("matcheLabelSelector  failed for matchExpItem:" + matchExprs);
                return false;
            }
        }
        return !matchExprs.isEmpty();
    }
}
