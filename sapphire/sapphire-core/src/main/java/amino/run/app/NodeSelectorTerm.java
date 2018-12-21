package amino.run.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by AmitRoushan on 03/19/19. which is kernel server selector term follows similar logic as
 * k8s
 */
public class NodeSelectorTerm implements Serializable {

    // A list of node selector requirements by node's labels.
    public List<Requirement> matchExpressions = new ArrayList<Requirement>();

    public void setMatchExpressions(List<Requirement> MatchExpressions) {
        this.matchExpressions = MatchExpressions;
    }

    public List<Requirement> getMatchExpressions() {
        return matchExpressions;
    }

    public NodeSelectorTerm add(Requirement requirement) {
        matchExpressions.add(requirement);
        return this;
    }
}
