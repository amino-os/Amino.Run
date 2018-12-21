package amino.run.app;

import java.io.Serializable;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

/**
 * Specification for node selection. Users use {@code NodeSelectorSpec} to specify on which nodes
 * (i.e. kernel servers) to run their microservice.
 *
 * <p>At present we only support specifying {@code NodeSelectorSpec} at microservice level. We will
 * consider support specifying {@code NodeSelectorSpec} at DM level in the future if necessary.
 */
public class NodeSelectorSpec implements Serializable {
    // If the affinity requirements specified by this field are not met at
    // scheduling time, the SO will not be scheduled onto the node.
    // +optional
    private List<NodeSelectorTerm> requireExpressions = new ArrayList<NodeSelectorTerm>();

    public void setRequireExpressions(List<NodeSelectorTerm> requireExpressions) {
        this.requireExpressions = requireExpressions;
    }

    public List<NodeSelectorTerm> getRequireExpressions() {
        return requireExpressions;
    }

    public NodeSelectorSpec addRequireExpressions(NodeSelectorTerm term) {
        requireExpressions.add(term);
        return this;
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeSelectorSpec that = (NodeSelectorSpec) o;
        return Objects.equals(requireExpressions, that.requireExpressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requireExpressions);
    }
}
