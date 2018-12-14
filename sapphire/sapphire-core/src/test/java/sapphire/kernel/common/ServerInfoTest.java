package sapphire.kernel.common;

import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class ServerInfoTest {
    private static String LABEL_PREFIX = "label_";
    private static String NON_EXISTENT_LABEL = "non_existent_label";
    private int numOfLabels = 5;

    @Test(expected = NullPointerException.class)
    public void addLabelsWithNull() {
        ServerInfo server = new ServerInfo(null);
        server.addLabels(null);
    }

    @Test
    public void testHasAnyLabel() {
        ServerInfo server = createServer(numOfLabels);
        Set<String> labels = new HashSet<>();
        labels.add(LABEL_PREFIX + "0");
        Assert.assertTrue(server.containsAny(labels));
    }

    @Test
    public void testHasAnyLabelWithNonExistentLabel() {
        ServerInfo server = createServer(numOfLabels);
        Set<String> labels = createLabels(numOfLabels);
        labels.add("non_existent_label");
        Assert.assertTrue(server.containsAny(labels));
    }

    @Test
    public void testHasAnyLabelWithEmptyLabels() {
        ServerInfo server = createServer(numOfLabels);
        Assert.assertTrue(server.containsAny(Collections.emptySet()));
    }

    @Test
    public void testHasAnyLabelFailure() {
        ServerInfo server = createServer(numOfLabels);
        Set<String> labels = new HashSet<>(Arrays.asList(NON_EXISTENT_LABEL));
        Assert.assertFalse(server.containsAny(labels));
    }

    @Test
    public void testHasAllLabel() {
        ServerInfo server = createServer(numOfLabels);
        Set<String> labels = createLabels(numOfLabels);
        Assert.assertTrue(server.containsAll(labels));
    }

    @Test
    public void testHasAllLabelFailure() {
        ServerInfo server = createServer(numOfLabels);
        Set<String> labels = createLabels(numOfLabels);
        labels.add(NON_EXISTENT_LABEL);
        Assert.assertFalse(server.containsAll(labels));
    }

    @Test
    public void testHasAllLabelsWithEmptyLabels() {
        ServerInfo server = createServer(numOfLabels);
        Assert.assertTrue(server.containsAll(Collections.emptySet()));
    }

    private ServerInfo createServer(int numOfLabels) {
        ServerInfo server = new ServerInfo(null);
        server.addLabels(createLabelsKS(numOfLabels));
        return server;
    }

    private Set<String> createLabels(int numOfLabels) {
        Set<String> labels = new HashSet<>();
        for (int i = 0; i < numOfLabels; i++) {
            labels.add(LABEL_PREFIX + i);
        }
        return labels;
    }

    private HashMap createLabelsKS(int numOfLabels) {
        HashMap labels = new HashMap();
        for (int i = 0; i < numOfLabels; i++) {
            labels.put(LABEL_PREFIX + i, LABEL_PREFIX + i);
        }
        return labels;
    }
}
