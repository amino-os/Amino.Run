package sapphire.kernel.common;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/** {@code ServerInfo} contains meta data of a kernel server. */
public class ServerInfo implements Serializable {
    private InetSocketAddress host;
    private String region;
    private Set<String> labels = new HashSet<>();

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

    public void addLabels(Set<String> labels) {
        if (labels == null) {
            throw new NullPointerException("Labels must not be null");
        }
        this.labels.addAll(labels);
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
            if (this.labels.contains(s)) {
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
     *     otherwise. Returns {@code true} if the given label set is {@code null} or empty.
     */
    public boolean containsAll(Set<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return true;
        }

        for (String s : labels) {
            if (!this.labels.contains(s)) {
                return false;
            }
        }
        return true;
    }
}
