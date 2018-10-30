package sapphire.kernel.common;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/** {@code ServerInfo} contains meta data of a kernel server. */
public class ServerInfo implements Serializable {
    public enum SERVER_TYPE {
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

    public void addLabel(String key, String val) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid blank key '%s'", key));
        }
        if (val == null || val.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid blank value '%s'", val));
        }
        labels.put(key, val);
    }

    public String removeLabel(String key) {
        return labels.remove(key);
    }

    public String getLabelValue(String key) {
        return labels.get(key);
    }

    public void addLabels(Map<String, String> labelMap) {
        if (labelMap == null) {
            throw new NullPointerException("labelMap not be null");
        }
        this.labels.putAll(labelMap);
    }
}
