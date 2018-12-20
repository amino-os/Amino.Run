package sapphire.app;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import sapphire.common.LabelUtils;

/**
 * Created by SrinivasChilveri on 12/14/18. which is kernel server selector term follows similar
 * logic as k8s
 */
public class NodeSelectorTerm implements Serializable {

    // A list of node selector requirements by node's labels.
    public List<NodeSelectorRequirement> MatchExpressions = null;
    // A list of node selector requirements by node's fields.
    // TODO currently its not used as we don't have a separate Node Fields
    // for each KS, once we add node fields for each ks then we can consider
    // this parameter for selection of the KS/Node
    public List<NodeSelectorRequirement> MatchFields = null;
    private static Logger logger = Logger.getLogger(NodeSelectorTerm.class.getName());

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
        if ((MatchFields == null) || (MatchFields.size() == 0)) {
            this.MatchFields = MatchFields;
            return;
        }
        for (int i = 0; i < MatchFields.size(); i++) {
            NodeSelectorRequirement filed = MatchFields.get(i);
            if (!LabelUtils.validateMatchFiledRequirement(
                    filed.getKey(), filed.getOperator(), filed.getValues())) {
                logger.severe("validateMatchFiledRequirement failed ");
                throw new IllegalArgumentException("Invalid input Argument not allowed " + filed);
            }
        }
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
