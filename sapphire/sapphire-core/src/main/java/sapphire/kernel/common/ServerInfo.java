package sapphire.kernel.common;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** {@code ServerInfo} contains meta data of a kernel server. */
public class ServerInfo implements Serializable {
    public enum NODE_TYPE {
        DEVICE,
        EDGE_SERVER,
        CLOUD_SERVER
    };

    private InetSocketAddress host;
    private String region;
    private Map<String, String> labels = new HashMap<>();

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

    public void addLabels(Map<String, String> labelMap) {
        if (labelMap == null) {
            throw new NullPointerException("labelMap not be null");
        }
        this.labels.putAll(labelMap);
    }

    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }
}
