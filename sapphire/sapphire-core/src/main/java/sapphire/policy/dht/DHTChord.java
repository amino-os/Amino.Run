package sapphire.policy.dht;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeSet;

public class DHTChord implements Serializable {
    private int virtualNodeFactor = 5;
    private TreeSet<DHTNode> nodes = new TreeSet<>(new DHTNodeComparator());
    private Random generator = new Random(System.currentTimeMillis());

    /** Default chord constructor with virtual node factor set to five. */
    public DHTChord() {}

    /**
     * Constructs a chord with the given virtual node factor. Virtual node factor must be greater
     * than zero.
     *
     * <p>{@code virtualNodeFactor} controls the number of virtual nodes to be added into the chord
     * for every server. If {@code virtualNodeFactor} is five, then five virtual nodes will be added
     * into chord for every server.
     *
     * @param virtualNodeFactor the number of virtual nodes to be added for every server. It must be
     *     greater than zero.
     */
    public DHTChord(int virtualNodeFactor) {
        if (virtualNodeFactor <= 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid virtual node factor %s. Virtual node factor must be greater than 0.",
                            virtualNodeFactor));
        }
        this.virtualNodeFactor = virtualNodeFactor;
    }

    /**
     * Adds the specified server into chord.
     *
     * <p>When {@code virtualNodeFactor} is specified, the chord will add the specified number of
     * virtual nodes for the given server in the chord.
     *
     * @param server {@code DHTServerPolicy} instance
     * @throws NullPointerException when server is {@code null}.
     */
    public void add(DHTPolicy.DHTServerPolicy server) {
        if (server == null) {
            throw new NullPointerException("server must not be null");
        }

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
