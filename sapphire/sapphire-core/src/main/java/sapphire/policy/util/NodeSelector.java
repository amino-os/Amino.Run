package sapphire.policy.util;

import java.util.HashMap;
import java.util.Map;

public class NodeSelector {
    // Labels for node selection. A node will
    // be selected if its label matches any
    // label in the list.
    private Map<String, String> nodeLabels;

    public NodeSelector() {
        nodeLabels = new HashMap<>();
    }

    public NodeSelector(Map<String, String> nodeLabels) {
        this();
        if (nodeLabels != null) {
            nodeLabels.putAll(nodeLabels);
        }
    }

    public void addNodeLabel(String key, String value) {
        nodeLabels.put(key, value);
    }

    public Map<String, String> getNodeLabels() {
        return this.nodeLabels;
    }
}
