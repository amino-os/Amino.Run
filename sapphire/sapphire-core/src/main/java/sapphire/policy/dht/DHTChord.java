package sapphire.policy.dht;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeSet;

public class DHTChord implements Serializable {
    private int virtualNodeFactor = 5;
    private TreeSet<DHTNode> nodes = new TreeSet<>(new DHTNodeComparator());
    private Random generator = new Random(System.currentTimeMillis());

    public DHTChord() {}

    public DHTChord(int virtualNodeFactor) {
        this.virtualNodeFactor = virtualNodeFactor;
    }

    public void add(DHTPolicy.DHTServerPolicy server) {
        for (int i = 0; i < virtualNodeFactor; i++) {
            DHTKey id = new DHTKey(Integer.toString(generator.nextInt(Integer.MAX_VALUE)));
            DHTNode node = new DHTNode(id, server);
            nodes.add(node);
        }
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
