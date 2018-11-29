package sapphire.kernel.common;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import sapphire.app.NodeSelectorRequirement;

/** {@code ServerInfo} contains meta data of a kernel server. */
public class ServerInfo implements Serializable {
    private InetSocketAddress host;
    // TODO: We need to make region a label too.
    // Keep region here for the time being to make
    // our codes backward compatible.
    private String region;
    private Map<String, String> labels = new HashMap<String, String>();
    private static Logger logger = Logger.getLogger(ServerInfo.class.getName());

    public ServerInfo(InetSocketAddress addr, String reg) {
        this.host = addr;
        this.region = reg;
    }

    public InetSocketAddress getHost() {
        return host;
    }

    public String getRegion() {
        return region;
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
     * @param labels a map of labels
     * @return {@code true} if the server contains any label in the label set; {@code false}
     *     otherwise. Returns {@code true} if the given label set is {@code null} or empty.
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
     * @return {@code true} if the server contains all labels in the label set; {@code false}
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
                logger.warning(
                        "containsAll return false as this server doesn't have the required labels: "
                                + entries.toString());
                return false;
            }
        }
        return true;
    }
    // TODO need to implement
    /**
     * Checks if the server contains <strong>all</strong> labels specified in the given label map.
     * If the specified label map is {@code null} or empty, we consider no selector is specified,
     * and therefore we return {@code true}.
     *
     * @param matchExprs a list of NodeSelectorRequirement
     * @return {@code true} if the server matches all matchExprs in the list; {@code false}
     *     otherwise. Returns {@code true} if the given matchExprs is {@code null} or empty.
     */
    public boolean matchLabelExpressions(List<NodeSelectorRequirement> matchExprs) {
        return true;
    }
}
