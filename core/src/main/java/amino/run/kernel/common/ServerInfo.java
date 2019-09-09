package amino.run.kernel.common;

import amino.run.app.NodeSelectorSpec;
import amino.run.app.NodeSelectorTerm;
import amino.run.app.Requirement;
import amino.run.kernel.metric.NodeMetric;
import amino.run.kernel.server.KernelServerImpl;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code ServerInfo} contains meta data of a kernel server. */
public class ServerInfo implements Serializable {
    private InetSocketAddress host;

    private Map<String, String> labels = new HashMap<String, String>();

    public int processorCount; // Available processor count

    public ServerInfo(InetSocketAddress addr) {
        this.host = addr;
        this.processorCount = Runtime.getRuntime().availableProcessors();
    }

    public InetSocketAddress getHost() {
        return host;
    }

    public String getRegion() {
        return labels.get(KernelServerImpl.REGION_KEY);
    }

    public transient Map<InetSocketAddress, NodeMetric> metrics;

    public void addLabels(Map keyValues) {
        if (keyValues == null) {
            throw new NullPointerException("Labels must not be null");
        }
        this.labels.putAll(keyValues);
    }

    /**
     * Check {@link ServerInfo} instance matches with node selection specifications
     *
     * <p>If node selection is null or node selection terms are empty, All kernel server get
     * selected If any node selection terms meet the kernel server labels, kernel server get
     * selected
     *
     * @param spec node selection specifications
     * @return {@code true} if server matches node selection specifications
     */
    public boolean matchNodeSelectorSpec(NodeSelectorSpec spec) {
        // if spec is empty , it mean accept all kernel server
        if (spec == null) {
            return true;
        }

        List<NodeSelectorTerm> terms = spec.getNodeSelectorTerms();
        // if terms is not empty then at least one term should meet server labels
        for (NodeSelectorTerm term : terms) {
            List<Requirement> requirements = term.getMatchRequirements();

            if (!matchRequirements(requirements)) {
                continue;
            }
            return true;
        }
        // if terms is empty , All kernel server get selected
        return terms.isEmpty();
    }

    private boolean matchRequirements(List<Requirement> requirements) {
        // all requirements should match
        for (Requirement requirement : requirements) {
            if (!requirement.matches(labels)) {
                return false;
            }
        }
        return !requirements.isEmpty();
    }

    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        s.defaultWriteObject();
        GlobalKernelReferences.nodeServer.getKernelClient().writeMetrics(s);
    }

    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        metrics = GlobalKernelReferences.nodeServer.getKernelClient().readMetrics(s);
    }
}
