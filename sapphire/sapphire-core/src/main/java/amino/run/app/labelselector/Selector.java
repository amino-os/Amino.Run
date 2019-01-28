package amino.run.app.labelselector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Selector hosts set of condition used for filtering labeled objects.
 *
 * <p>Each Selector can have any number of conditions. An object get selected if all selection
 * condition return true.
 */
public class Selector implements Serializable {
    private ArrayList<Requirement> requirements = new ArrayList<>();

    /**
     * Test a label against all Selector conditions
     *
     * @param labels instance of {@link Labels}
     * @return true if a label matches all conditions
     */
    public boolean matches(Labels labels) {
        if (labels == null) {
            return false;
        }
        for (Requirement requirement : requirements) {
            if (!requirement.matches(labels)) {
                return false;
            }
        }
        return !requirements.isEmpty();
    }

    /**
     * Add conditions in Selector
     *
     * @param requirements List of conditions
     * @return Selector instance
     */
    public Selector add(Requirement... requirements) {
        this.requirements.addAll(Arrays.asList(requirements));
        return this;
    }

    @Override
    public String toString() {
        List<String> selectors = new ArrayList<>();
        for (Requirement requirement : requirements) {
            selectors.add(requirement.toString());
        }

        return String.join(",", selectors);
    }
}
