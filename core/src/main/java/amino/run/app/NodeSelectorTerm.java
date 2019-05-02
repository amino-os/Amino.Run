package amino.run.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by AmitRoushan on 03/19/19. which is kernel server selector term follows similar logic as
 * k8s
 */
public class NodeSelectorTerm implements Serializable {

    // A list of node selector requirements by node's labels.
    private List<Requirement> matchRequirements = new ArrayList<Requirement>();

    /**
     * Set requirements required to match
     *
     * @param requirements set of requirements
     */
    public void setMatchRequirements(List<Requirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            throw new IllegalArgumentException("empty requirement for node selection terms");
        }

        this.matchRequirements = requirements;
        validate();
    }

    /**
     * Get list of requirements
     *
     * @return list of requirements
     */
    public List<Requirement> getMatchRequirements() {
        return matchRequirements;
    }

    /**
     * Update {@code matchRequirements} with new list of requirements
     *
     * @param requirements list of requirements
     * @return
     */
    public void addMatchRequirements(Requirement... requirements) {
        matchRequirements.addAll(Arrays.asList(requirements));
        validate();
    }

    /**
     * Validate requirements for node selection terms
     *
     * @throws IllegalArgumentException
     */
    public void validate() throws IllegalArgumentException {
        for (Requirement requirement : matchRequirements) {
            requirement.validateRequirement();
        }
    }
}
