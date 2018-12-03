package sapphire.app;

import java.io.Serializable;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

public class PreferredSchedulingTerm implements Serializable {

    private int weight;
    private NodeSelectorTerm preference;

    public void setweight(int weight) {
        this.weight = weight;
    }

    public int getweight() {
        return weight;
    }

    public NodeSelectorTerm getNodeSelectorTerm() {
        return preference;
    }

    public void setNodeSelectorTerm(NodeSelectorTerm preference) {
        this.preference = preference;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PreferredSchedulingTerm term = (PreferredSchedulingTerm) o;
        return Objects.equals(this.weight, term.weight)
                && Objects.equals(this.preference, term.preference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weight, preference);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }
}
