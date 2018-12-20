package sapphire.app;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import sapphire.common.LabelUtils;

public class NodeSelectorTermTest {
    @Test
    public void testsetMatchFieldsWithNull() {
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.setMatchFields(null);
    }

    @Test
    public void testsetMatchFieldsWithValidData() {
        NodeSelectorTerm term = new NodeSelectorTerm();
        String[] values3 = {"val31"};
        List<String> vals3 = Arrays.asList(values3);
        NodeSelectorRequirement matchExpItem3 =
                new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals3);
        matchExpItem3.hashCode();
        List<NodeSelectorRequirement> MatchFields = Arrays.asList(matchExpItem3);
        term.setMatchFields(MatchFields);
        term.hashCode();
        Assert.assertTrue(term.getMatchFields().equals(MatchFields));
    }

    @Test
    public void testsetMatchFieldsWithInvData() {
        NodeSelectorTerm term = new NodeSelectorTerm();
        String[] values3 = {"val31"};
        List<String> vals3 = Arrays.asList(values3);
        // matchFiled allows only In & Not In operator
        NodeSelectorRequirement matchExpItem3 =
                new NodeSelectorRequirement("key1", LabelUtils.Equals, vals3);
        List<NodeSelectorRequirement> MatchFields = Arrays.asList(matchExpItem3);
        try {
            term.setMatchFields(MatchFields);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            Assert.fail("Should not execute this because should throw IllegalArgumentException");
        }
    }

    @Test
    public void testsetMatchExpressionsWithNull() {
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.setMatchExpressions(null);
    }
}
