package sapphire.app;

import java.io.Serializable;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

public class NodeSelectorTerm implements Serializable {

    // A list of node selector requirements by node's labels.
    public List<NodeSelectorRequirement> MatchExpressions = null;
    // A list of node selector requirements by node's fields.
    public List<NodeSelectorRequirement> MatchFields = null;

    public void setMatchExpressions(List<NodeSelectorRequirement> MatchExpressions) {
        this.MatchExpressions = MatchExpressions;
    }

    public List<NodeSelectorRequirement> getMatchExpressions() {
        return MatchExpressions;
    }

    public List<NodeSelectorRequirement> getMatchFields() {
        return MatchFields;
    }

    public void setMatchFields(List<NodeSelectorRequirement> MatchFields) {
        this.MatchFields = MatchFields;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeSelectorTerm term = (NodeSelectorTerm) o;
        return Objects.equals(this.MatchExpressions, term.MatchExpressions)
                && Objects.equals(this.MatchFields, term.MatchFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(MatchExpressions, MatchFields);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }
}
