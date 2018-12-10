package sapphire.oms;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.app.*;
import sapphire.common.LabelUtils;
import sapphire.kernel.common.ServerInfo;

public class KernelServerManagerTest {
    private static final String LABEL1_PREFIX = "label1_";
    private static final String LABEL2_PREFIX = "label2_";
    private static final String NON_EXISTENT_LABEL = "non_existent_label";
    private int numOfServers = 10;
    private KernelServerManager manager;

    @Before
    public void setup() throws Exception {
        manager = new KernelServerManager();
        registerServers(manager, numOfServers);
    }

    @Test
    public void testmatchLabelSetWithSingleLabel() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addMatchLabelsItem(LABEL1_PREFIX + "1", LABEL1_PREFIX + "1");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    @Test
    public void testmatchLabelSetWithMultiLabels() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addMatchLabelsItem(LABEL1_PREFIX + "1", LABEL1_PREFIX + "1");
        spec.addMatchLabelsItem(LABEL2_PREFIX + "1", LABEL2_PREFIX + "1");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    @Test
    public void testmatchLabelSetWithNonExistingLabel() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addMatchLabelsItem(NON_EXISTENT_LABEL, NON_EXISTENT_LABEL);
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testEmptyLabelSet() throws Exception {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(10, result.size());
    }

    @Test
    public void testNodeAffinityWithMatchExpressionsNotInOp() throws Exception {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeAffinity nodeAffinity = new NodeAffinity();
        NodeSelectorTerm term = new NodeSelectorTerm();

        String[] values = {"val1", "val2", "val3"};
        List<String> vals1 = Arrays.asList(values);
        try {
            NodeSelectorRequirement matchExpItem =
                    new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals1);
            List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem);

            term.setMatchExpressions(MatchExpressions1);
            List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term);

            nodeAffinity.setRequireExpressions(RequireExpressions);

            spec.setNodeAffinity(nodeAffinity);
        } catch (Exception e) {
            Assert.fail();
        }
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(10, result.size());
    }

    @Test
    public void testNodeAffinityWithMatchExpressionsInOp() throws Exception {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeAffinity nodeAffinity = new NodeAffinity();
        NodeSelectorTerm term = new NodeSelectorTerm();

        String[] values = {"label1_0", "val2", "val3"};
        List<String> vals1 = Arrays.asList(values);
        try {
            NodeSelectorRequirement matchExpItem =
                    new NodeSelectorRequirement("label1_0", LabelUtils.In, vals1);
            List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem);

            term.setMatchExpressions(MatchExpressions1);
            List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term);

            nodeAffinity.setRequireExpressions(RequireExpressions);

            spec.setNodeAffinity(nodeAffinity);
        } catch (Exception e) {
            Assert.fail();
        }
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testNodeAffinityWithMatchExpressionsNotInOpPriority() throws Exception {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeAffinity nodeAffinity = new NodeAffinity();
        NodeSelectorTerm term = new NodeSelectorTerm();
        NodeSelectorTerm Prioterm = new NodeSelectorTerm();
        PreferredSchedulingTerm prefterm = new PreferredSchedulingTerm();

        String[] values = {"val1", "val2", "val3"};
        List<String> vals1 = Arrays.asList(values);

        String[] values2 = {"label1_5", "val2", "val3"};
        List<String> vals2 = Arrays.asList(values2);

        try {
            NodeSelectorRequirement matchExpItem1 =
                    new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals1);
            List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem1);
            term.setMatchExpressions(MatchExpressions1);
            List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term);
            NodeSelectorRequirement matchExpItem2 =
                    new NodeSelectorRequirement("label1_5", LabelUtils.In, vals2);
            List<NodeSelectorRequirement> MatchExpressions2 = Arrays.asList(matchExpItem2);

            // label1_5 is used for priority
            Prioterm.setMatchExpressions(MatchExpressions2);
            prefterm.setNodeSelectorTerm(Prioterm);
            prefterm.setweight(1);

            List<PreferredSchedulingTerm> PreferScheduling = Arrays.asList(prefterm);
            nodeAffinity.setRequireExpressions(RequireExpressions);

            nodeAffinity.setPreferScheduling(PreferScheduling);

            spec.setNodeAffinity(nodeAffinity);
        } catch (Exception e) {
            Assert.fail();
        }
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(10, result.size());
        InetSocketAddress host = manager.chooseBestAmong(result, spec);
        ServerInfo s = manager.getServerInfo(host);
        Assert.assertTrue(s.getRegion().equals("region_5"));
    }

    private void registerServers(KernelServerManager manager, int numOfServers) throws Exception {
        for (int i = 0; i < numOfServers; i++) {
            ServerInfo s = new ServerInfo(new InetSocketAddress(i), "region_" + i);
            HashMap labels = new HashMap();
            labels.put(LABEL1_PREFIX + i, LABEL1_PREFIX + i);
            labels.put(LABEL2_PREFIX + i, LABEL2_PREFIX + i);
            s.addLabels(labels);
            manager.registerKernelServer(s);
        }
    }
}
