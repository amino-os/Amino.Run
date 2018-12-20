package sapphire.app;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import sapphire.common.LabelUtils;
import sapphire.common.Utils;

public class NodeSelectorSpecTest {

    @Test
    public void testsetNullAffinity() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.setNodeAffinity(null);
    }

    @Test
    public void testnodeSelectorSpec() throws Exception {
        NodeSelectorSpec nodeSelectorSpec = new NodeSelectorSpec();
        String[] values1 = {"val11", "val12", "val13"};
        String[] values2 = {"val21", "val22", "val23"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem1 =
                new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals1);
        List<String> vals2 = Arrays.asList(values2);
        NodeSelectorRequirement matchExpItem2 =
                new NodeSelectorRequirement("key2", LabelUtils.NotIn, vals2);
        List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem1);
        List<NodeSelectorRequirement> MatchExpressions2 = Arrays.asList(matchExpItem2);
        NodeSelectorTerm term1 = new NodeSelectorTerm();
        term1.setMatchExpressions(MatchExpressions1);
        Assert.assertTrue(term1.toString().equals(term1.toString()));
        NodeSelectorTerm term2 = new NodeSelectorTerm();
        term2.setMatchExpressions(MatchExpressions2);
        PreferredSchedulingTerm prefterm = new PreferredSchedulingTerm();
        prefterm.setNodeSelectorTerm(term2);
        prefterm.setweight(1);
        Assert.assertTrue(prefterm.toString().equals(prefterm.toString()));
        List<PreferredSchedulingTerm> PreferSchedulingterms = Arrays.asList(prefterm);
        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term1);
        KsAffinity nodeAffinity = new KsAffinity();
        nodeAffinity.setPreferScheduling(PreferSchedulingterms);
        nodeAffinity.setRequireExpressions(RequireExpressions);
        Assert.assertTrue(nodeAffinity.toString().equals((nodeAffinity.toString())));
        nodeSelectorSpec.setNodeAffinity(nodeAffinity);

        NodeSelectorSpec clone = (NodeSelectorSpec) Utils.toObject(Utils.toBytes(nodeSelectorSpec));
        Assert.assertEquals(nodeSelectorSpec, clone);

        Assert.assertTrue(nodeSelectorSpec.toString().equals(nodeSelectorSpec.toString()));
    }
}
