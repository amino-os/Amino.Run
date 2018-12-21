package sapphire.app.labelselector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Selector implements Serializable {
    private ArrayList<Requirement> requirements = new ArrayList<>();

    public boolean matches(Labels labels) {
        for (Requirement requirement : requirements) {
            if (!requirement.matches(labels)) {
                return false;
            }
        }
        return !requirements.isEmpty();
    }

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
