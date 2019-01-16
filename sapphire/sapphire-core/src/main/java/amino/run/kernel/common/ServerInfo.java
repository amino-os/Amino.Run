package amino.run.kernel.common;

import amino.run.kernel.server.KernelServerImpl;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    public boolean containsAny(Set<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }

        for (String s : labels) {
            if (this.labels.containsValue(s)) {
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
    public boolean containsAll(Set<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }
        for (String s : labels) {
            if (!this.labels.containsValue(s)) {
                logger.warning(
                        "containsAll return false as this server doesn't have the required labels: "
                                + s);
                return false;
            }
        }
        return true;
    }
}
