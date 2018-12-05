package sapphire.common;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import sapphire.app.NodeAffinity;
import sapphire.app.NodeSelectorRequirement;
import sapphire.app.NodeSelectorTerm;
import sapphire.app.PreferredSchedulingTerm;

/** Created by Srinivas on 12/03/18. */
public class LabelUtilsTest {

    @Test
    public void testvalidateLabelKey_nullKey() {
        String key = null;
        Assert.assertFalse(LabelUtils.validateLabelKey(key));
    }

    @Test
    public void testvalidateLabelKey_InvKey() {
        // lenght is more than 63
        String key = "aaaaaaaaaaaaaaaaaaaabbbbbbbbbbbccccccccccccddddddddddddeeeeeeeee";
        Assert.assertFalse(LabelUtils.validateLabelKey(key));
    }

    @Test
    public void testvalidateLabelKey_ValKey() {
        String key = "abc.def.com";
        Assert.assertTrue(LabelUtils.validateLabelKey(key));
    }

    @Test
    public void testvalidateLabelValue_nullVal() {
        String val = null;
        Assert.assertFalse(LabelUtils.validateLabelValue(val));
    }

    @Test
    public void testvalidateLabelValue_InvVal() {
        // lenght is more than 63
        String val = "aaaaaaaaaaaaaaaaaaaabbbbbbbbbbbccccccccccccddddddddddddeeeeeeeee";
        Assert.assertFalse(LabelUtils.validateLabelValue(val));
    }

    @Test
    public void testvalidateLabelValue_Val() {
        String val = "abc";
        Assert.assertTrue(LabelUtils.validateLabelValue(val));
    }

    @Test
    public void testvalidateNodeSelectRequirement_nullKey() {
        String key = null;
        String[] values = {"abc"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.In, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_nullOp() {
        String key = "key";
        String[] values = {key};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, null, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_InvOp() {
        String key = "key";
        String[] values = {key};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, key, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_valZeroInOP() {
        String key = "key";
        String[] values = {};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.In, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_valZeroEqlOP() {
        String key = "key";
        String[] values = {};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.Equals, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_val2EqlOP() {
        String key = "key";
        String[] values = {"key", "key2"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.Equals, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_val2ExistsOP() {
        String key = "key";
        String[] values = {"key", "key2"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(
                LabelUtils.validateNodeSelectRequirement(key, LabelUtils.GreaterThan, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_Invval1ExistsOP() {
        String key = "key";
        String[] values = {"key"};
        List<String> vals = Arrays.asList(values);
        Assert.assertFalse(
                LabelUtils.validateNodeSelectRequirement(key, LabelUtils.GreaterThan, vals));
    }

    @Test
    public void testvalidateNodeSelectRequirement_Succ() {
        String key = "key";
        String[] values = {key};
        List<String> vals = Arrays.asList(values);
        Assert.assertTrue(LabelUtils.validateNodeSelectRequirement(key, LabelUtils.In, vals));
    }

    @Test
    public void testvalidateNodeAffinity_null() {
        Assert.assertTrue(LabelUtils.validateNodeAffinity(null));
    }

    @Test
    public void testvalidateNodeAffinity() {
        NodeAffinity nodeAffinity = new NodeAffinity();

        String[] values1 = {"val1", "val2", "val3"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem =
                new NodeSelectorRequirement("key1", LabelUtils.In, vals1);

        List<NodeSelectorRequirement> MatchExpressions = Arrays.asList(matchExpItem);

        NodeSelectorTerm term = new NodeSelectorTerm();
        term.setMatchExpressions(MatchExpressions);

        PreferredSchedulingTerm prefterm = new PreferredSchedulingTerm();
        prefterm.setNodeSelectorTerm(term);
        prefterm.setweight(1);

        List<PreferredSchedulingTerm> PreferSchedulingterms = Arrays.asList(prefterm);

        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term);

        nodeAffinity.setPreferScheduling(PreferSchedulingterms);
        nodeAffinity.setRequireExpressions(RequireExpressions);

        Assert.assertTrue(LabelUtils.validateNodeAffinity(nodeAffinity));
    }

    @Test
    public void testvalidateNodeAffinityInvPref() {
        NodeAffinity nodeAffinity = new NodeAffinity();

        String[] values1 = {"val1", "val2", "val3"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem =
                new NodeSelectorRequirement("key1", LabelUtils.In, vals1);

        List<NodeSelectorRequirement> MatchExpressions = Arrays.asList(matchExpItem);

        NodeSelectorTerm term = new NodeSelectorTerm();
        term.setMatchExpressions(MatchExpressions);

        PreferredSchedulingTerm prefterm = new PreferredSchedulingTerm();
        prefterm.setNodeSelectorTerm(term);
        prefterm.setweight(-1);

        List<PreferredSchedulingTerm> PreferSchedulingterms = Arrays.asList(prefterm);

        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term);

        nodeAffinity.setPreferScheduling(PreferSchedulingterms);
        nodeAffinity.setRequireExpressions(RequireExpressions);

        Assert.assertFalse(LabelUtils.validateNodeAffinity(nodeAffinity));
    }

    @Test
    public void testvalidateNodeAffinityInvReq() {
        NodeAffinity nodeAffinity = new NodeAffinity();

        String[] values1 = {"val1", "val2", "val3"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem = new NodeSelectorRequirement();

        List<NodeSelectorRequirement> MatchExpressions = Arrays.asList(matchExpItem);

        NodeSelectorTerm term = new NodeSelectorTerm();
        term.setMatchExpressions(MatchExpressions);
        term.setMatchFields(null);

        PreferredSchedulingTerm prefterm = new PreferredSchedulingTerm();
        prefterm.setNodeSelectorTerm(term);
        prefterm.setweight(1);

        List<PreferredSchedulingTerm> PreferSchedulingterms = Arrays.asList(prefterm);

        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term);

        nodeAffinity.setPreferScheduling(PreferSchedulingterms);
        nodeAffinity.setRequireExpressions(RequireExpressions);

        Assert.assertFalse(LabelUtils.validateNodeAffinity(nodeAffinity));
    }
}
