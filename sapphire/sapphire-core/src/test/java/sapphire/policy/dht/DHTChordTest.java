package sapphire.policy.dht;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DHTChordTest {
    private DHTChord dhtChord;

    @Before
    public void setup() {
        this.dhtChord = new DHTChord();

        for (int i = 0; i < 5; i++) {
            DHTNode ni = new DHTNode(new DHTKey("node_" + i), null);
            dhtChord.add(ni);
        }
    }

    @Test
    public void testGetResponsibleNodeWhenNodeExists() {
        String identifier = "node_0";
        DHTNode n = dhtChord.getResponsibleNode(new DHTKey(identifier));
        // DHTKey exists in nodes
        Assert.assertEquals(identifier, n.id.getIdentifier());
    }

    @Test
    public void testGetResponsibleNodeWhenNodeNotExists() {
        TreeSet<DHTNode> nodes = dhtChord.getNodes();
        DHTKey key = new DHTKey("node_x");
        DHTNode node = dhtChord.getResponsibleNode(key);

        if (node.id.compareTo(key) < 0) {
            // If node.id is smaller than key, then node.id must be the
            // largest one that is smaller than key.
            // Verify that there is no node whose id is greater
            // than node.id and is less than key.
            for (DHTNode i : nodes) {
                Assert.assertFalse((i.id.compareTo(node.id) > 0) && (i.id.compareTo(key) < 0));
            }
        } else {
            // If node.id is greater than key, then this node.id
            // must be the largest id among all nodes.

            // Verify that node.is is the largest among all nodes
            for (DHTNode i : nodes) {
                Assert.assertTrue(node.id.compareTo(i.id) >= 0);
            }

            // Verify that there is no node whose id is less than key
            for (DHTNode i : nodes) {
                Assert.assertTrue(i.id.compareTo(key) > 0);
            }
        }
    }

    @Test
    public void testSerializeChord() throws Exception {
        DHTChord chord = new DHTChord();
        byte[] bytes = serialize(chord);
        DHTChord clone = (DHTChord) deserizlize(bytes);
        System.out.println(clone);
    }

    private byte[] serialize(Object obj) throws Exception {
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(obj);
            return bout.toByteArray();
        }
    }

    private Object deserizlize(byte[] bytes) throws Exception {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
                ObjectInputStream in = new ObjectInputStream(bin)) {
            return in.readObject();
        }
    }
}
