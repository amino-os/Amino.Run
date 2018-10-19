package sapphire.policy;

import java.util.Map;
import sapphire.policy.util.NodeSelector;

public class SapphireLocationConfig implements SapphirePolicyConfig {
    public static final String REGION = "region";
    private NodeSelector nodeSelector;

    public SapphireLocationConfig() {
        nodeSelector = new NodeSelector();
    }

    public SapphireLocationConfig(SapphireLocationConfig config) {
        nodeSelector = new NodeSelector(config.getNodeLabels());
    }

    public void addNodeLabel(String key, String value) {
        nodeSelector.addNodeLabel(key, value);
    }

    public Map<String, String> getNodeLabels() {
        return nodeSelector.getNodeLabels();
    }
}
