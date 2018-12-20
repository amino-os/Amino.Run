package sapphire.app;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;

/**
 * Created by SrinivasChilveri on 12/14/18. which is kernel server affinity follows similar logic as
 * k8s
 */

// Kernel Server affinity is a group of ks affinity scheduling rules.
public class KsAffinity implements Serializable {

    // If the affinity requirements specified by this field are not met at
    // scheduling time, the SO will not be scheduled onto the node.
    // +optional
    private List<NodeSelectorTerm> RequireExpressions = null;
    // The scheduler will prefer to schedule SOs to nodes that satisfy
    // the affinity expressions specified by this field, but it may choose
    // a node that violates one or more of the expressions. The node that is
    // most preferred is the one with the greatest sum of weights, i.e.
    // for each node that meets all of the scheduling requirements (resource
    // request, requiredDuringScheduling affinity expressions, etc.),
    // compute a sum by iterating through the elements of this field and adding
    // "weight" to the sum if the node matches the corresponding matchExpressions; the
    // node(s) with the highest sum are the most preferred.
    // +optional
    private List<PreferredSchedulingTerm> PreferScheduling = null;

    public void setRequireExpressions(List<NodeSelectorTerm> RequireExpressions) {
        this.RequireExpressions = RequireExpressions;
    }

    public List<NodeSelectorTerm> getRequireExpressions() {
        return RequireExpressions;
    }

    public List<PreferredSchedulingTerm> getPreferScheduling() {
        return PreferScheduling;
    }

    public void setPreferScheduling(List<PreferredSchedulingTerm> PreferScheduling) {
        this.PreferScheduling = PreferScheduling;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KsAffinity term = (KsAffinity) o;
        return Objects.equals(this.RequireExpressions, term.RequireExpressions)
                && Objects.equals(this.PreferScheduling, term.PreferScheduling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(RequireExpressions, PreferScheduling);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }
}
