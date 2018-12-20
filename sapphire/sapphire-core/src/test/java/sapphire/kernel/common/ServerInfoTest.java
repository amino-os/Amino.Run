package sapphire.kernel.common;

import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import sapphire.app.*;
import sapphire.common.LabelUtils;

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
        HashMap labels = new HashMap();
        labels.put(LABEL_PREFIX + "0", LABEL_PREFIX + "0");
        Assert.assertTrue(server.containsAny(labels));
    }

    @Test
    public void testHasAnyLabelWithNonExistentLabel() {
        ServerInfo server = createServer(numOfLabels);
        HashMap labels = createLabels(numOfLabels);
        labels.put("non_existent_label", "non_existent_label");
        Assert.assertTrue(server.containsAny(labels));
    }

    @Test
    public void testHasAnyLabelWithEmptyLabels() {
        ServerInfo server = createServer(numOfLabels);
        Assert.assertTrue(server.containsAny(new HashMap()));
    }

    @Test
    public void testHasAnyLabelFailure() {
        ServerInfo server = createServer(numOfLabels);
        HashMap labels = new HashMap();
        labels.put(NON_EXISTENT_LABEL, NON_EXISTENT_LABEL);
        Assert.assertFalse(server.containsAny(labels));
    }

    @Test
    public void testHasAllLabel() {
        ServerInfo server = createServer(numOfLabels);
        HashMap labels = createLabels(numOfLabels);
        Assert.assertTrue(server.containsAll(labels));
    }

    @Test
    public void testHasAllLabelFailure() {
        ServerInfo server = createServer(numOfLabels);
        HashMap labels = createLabels(numOfLabels);
        labels.put(NON_EXISTENT_LABEL, NON_EXISTENT_LABEL);
        Assert.assertFalse(server.containsAll(labels));
    }

    @Test
    public void testmatchNodeAffinityWithSingleExprs() {
        ServerInfo server = createServer(numOfLabels);
        KsAffinity nodeAffinity = new KsAffinity();
        String[] values1 = {"val11", LABEL_PREFIX + "0", "val13"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem1 =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.In, vals1);
        List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem1);
        NodeSelectorTerm term1 = new NodeSelectorTerm();
        term1.setMatchExpressions(MatchExpressions1);
        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term1);
        nodeAffinity.setRequireExpressions(RequireExpressions);
        Assert.assertTrue(server.matchNodeAffinity(nodeAffinity));
    }

    @Test
    public void testmatchNodeAffinityWithAndExprs() {
        ServerInfo server = createServer(numOfLabels);
        KsAffinity nodeAffinity = new KsAffinity();
        String[] values1 = {"val11", LABEL_PREFIX + "0", "val13"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem1 =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.In, vals1);
        NodeSelectorRequirement matchExpItem2 =
                new NodeSelectorRequirement(LABEL_PREFIX + "1", LabelUtils.In, vals1);
        // Expressions are Anded so all Expressions should matche then only matchNodeAffinity
        // returns true
        List<NodeSelectorRequirement> MatchExpressions1 =
                Arrays.asList(matchExpItem1, matchExpItem2);
        NodeSelectorTerm term1 = new NodeSelectorTerm();
        term1.setMatchExpressions(MatchExpressions1);
        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term1);
        nodeAffinity.setRequireExpressions(RequireExpressions);
        Assert.assertFalse(server.matchNodeAffinity(nodeAffinity));
    }

    @Test
    public void testmatchNodeAffinityWithOrTerms() {
        ServerInfo server = createServer(numOfLabels);
        KsAffinity nodeAffinity = new KsAffinity();
        String[] values1 = {"val11", LABEL_PREFIX + "0", "val13"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem1 =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.In, vals1);
        NodeSelectorRequirement matchExpItem2 =
                new NodeSelectorRequirement(LABEL_PREFIX + "1", LabelUtils.In, vals1);
        List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem1);
        NodeSelectorTerm term1 = new NodeSelectorTerm();
        term1.setMatchExpressions(MatchExpressions1);
        List<NodeSelectorRequirement> MatchExpressions2 = Arrays.asList(matchExpItem2);
        NodeSelectorTerm term2 = new NodeSelectorTerm();
        term2.setMatchExpressions(MatchExpressions2);
        // terms are Ored so any one term matches matchNodeAffinity returns true
        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term2, term1);
        nodeAffinity.setRequireExpressions(RequireExpressions);
        Assert.assertTrue(server.matchNodeAffinity(nodeAffinity));
    }

    @Test
    public void testmatcheLabelSelectorWithDoubleEq() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {LABEL_PREFIX + "0"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.DoubleEquals, vals1);
        Assert.assertTrue(server.matcheLabelSelector(req));
    }

    @Test
    public void testmatcheLabelSelectorWithGt() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {"0"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.GreaterThan, vals1);
        Assert.assertFalse(server.matcheLabelSelector(req));
    }

    @Test
    public void testmatcheLabelSelectorWithExists() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.Exists, vals1);
        Assert.assertTrue(server.matcheLabelSelector(req));
    }

    @Test
    public void testmatcheFieldSelectorMorethanOneVal() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {"val11", LABEL_PREFIX + "0", "val13"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.In, vals1);
        Assert.assertFalse(server.matcheFieldSelector(req));
    }

    @Test
    public void testmatcheFieldSelectorInvalidOp() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {"val11"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.Equals, vals1);
        Assert.assertFalse(server.matcheFieldSelector(req));
    }

    @Test
    public void testmatcheFieldSelectorInOp() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {LABEL_PREFIX + "0"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.In, vals1);
        Assert.assertTrue(server.matcheFieldSelector(req));
    }

    @Test
    public void testmatchFiledsNotInOp() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {LABEL_PREFIX + "0"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.NotIn, vals1);
        List<NodeSelectorRequirement> matchFields = Arrays.asList(req);
        Assert.assertFalse(server.matchFileds(matchFields));
    }

    @Test
    public void testnullmatchFileds() {
        ServerInfo server = createServer(numOfLabels);
        Assert.assertTrue(server.matchFileds(null));
    }

    @Test
    public void testmatchFileds_Valid() {
        ServerInfo server = createServer(numOfLabels);
        String[] values1 = {LABEL_PREFIX + "0"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement req =
                new NodeSelectorRequirement(LABEL_PREFIX + "0", LabelUtils.In, vals1);
        List<NodeSelectorRequirement> matchFields = Arrays.asList(req);
        Assert.assertTrue(server.matchFileds(matchFields));
    }

    @Test
    public void testHasAllLabelsWithEmptyLabels() {
        ServerInfo server = createServer(numOfLabels);
        Assert.assertTrue(server.containsAll(new HashMap()));
    }

    private ServerInfo createServer(int numOfLabels) {
        ServerInfo server = new ServerInfo(null);
        server.addLabels(createLabels(numOfLabels));
        return server;
    }

    private HashMap createLabels(int numOfLabels) {
        HashMap labels = new HashMap();
        for (int i = 0; i < numOfLabels; i++) {
            labels.put(LABEL_PREFIX + i, LABEL_PREFIX + i);
        }
        return labels;
    }
}
