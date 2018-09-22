package sapphire.policy.dht;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeSet;

public class DHTChord implements Serializable {
    private TreeSet<DHTNode> nodes = new TreeSet<>(new DHTNodeComparator());

    public void add(DHTNode node) {
        nodes.add(node);
    }

    public DHTNode getResponsibleNode(DHTKey key) {
        DHTNode responsibleNode;
        DHTNode temp = new DHTNode(key, null);

        if (nodes.contains(temp)) {
            responsibleNode = nodes.tailSet(temp).first();
        } else {
            responsibleNode = nodes.lower(temp);
        }

        if (responsibleNode == null) responsibleNode = nodes.last();

        return responsibleNode;
    }

    public TreeSet<DHTNode> getNodes() {
        return this.nodes;
    }

    private static class DHTNodeComparator implements Serializable, Comparator<DHTNode> {
        @Override
        public int compare(DHTNode o1, DHTNode o2) {
            return o1.getId().compareTo(o2.getId());
        }
    }
}
