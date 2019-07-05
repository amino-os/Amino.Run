package amino.run.app;

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.yaml.snakeyaml.Yaml;

/**
 * Specification for node selection. Users use {@code NodeSelectorSpec} to specify on which nodes
 * (i.e. kernel servers) to run their microservice.
 *
 * <p>At present we only support specifying {@code NodeSelectorSpec} at microservice level. We will
 * consider support specifying {@code NodeSelectorSpec} at DM level in the future if necessary.
 */
public class NodeSelectorSpec implements Serializable {
    // If the requirements specified by this field are not met at
    // scheduling time, the MicroService will not be scheduled onto the node.
    // +optional
    private List<NodeSelectorTerm> nodeSelectorTerms = new ArrayList<NodeSelectorTerm>();

    /**
     * Set node selection terms
     *
     * <p>Method is also used by snakeyaml as JavaBean for MicroServiceSpec yaml parsing
     *
     * @param terms
     */
    public void setNodeSelectorTerms(List<NodeSelectorTerm> terms) {
        if (terms == null) {
            throw new IllegalArgumentException("node selection terms can not be null");
        }

        this.nodeSelectorTerms = terms;
        validate();
    }

    /**
     * Get node selection terms
     *
     * <p>Method is also used by snakeyaml as JavaBean for MicroServiceSpec yaml parsing
     *
     * @return List of node selection terms
     */
    public List<NodeSelectorTerm> getNodeSelectorTerms() {
        return nodeSelectorTerms;
    }

    /**
     * Update node selection terms with new term
     *
     * @param term
     * @return
     */
    public void addNodeSelectorTerms(NodeSelectorTerm term) {
        term.validate();
        nodeSelectorTerms.add(term);
    }

    /**
     * Validate node selection terms
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        for (NodeSelectorTerm term : nodeSelectorTerms) {
            term.validate();
        }
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
        return Objects.equal(nodeSelectorTerms, that.nodeSelectorTerms);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodeSelectorTerms);
    }
}
