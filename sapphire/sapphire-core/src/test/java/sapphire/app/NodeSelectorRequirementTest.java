package sapphire.app;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import sapphire.common.LabelUtils;

public class NodeSelectorRequirementTest {
    @Test
    public void testNullsetKey() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        try {
            spec.setKey(null);
            Assert.fail("Should not execute this because should throw Exception");
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            Assert.fail("Should not execute this because should throw IllegalArgumentException");
        }
    }

    @Test
    public void testsetOperator() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        try {
            spec.setOperator(null);
            Assert.fail("Should not execute this because should throw Exception");
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            Assert.fail("Should not execute this because should throw IllegalArgumentException");
        }
    }

    @Test
    public void testsetValues() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        try {
            spec.setValues(null);
            Assert.fail("Should not execute this because should throw Exception");
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            Assert.fail("Should not execute this because should throw IllegalArgumentException");
        }
    }

    @Test
    public void testaddValuesItem() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        try {
            spec.addValuesItem(null);
            Assert.fail("Should not execute this because should throw Exception");
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            Assert.fail("Should not execute this because should throw IllegalArgumentException");
        }
    }

    @Test
    public void testNodeSelectorRequirement() {
        String key = "key1";
        String[] values = {"val1", "val2", "val3"};
        List<String> vals = Arrays.asList(values);
        NodeSelectorRequirement matchExp = new NodeSelectorRequirement();
        matchExp.setKey(key);
        matchExp.setOperator(LabelUtils.Equals);
        matchExp.setValues(vals);

        Assert.assertTrue(matchExp.getKey().equals(key));
        Assert.assertTrue(matchExp.getOperator().equals(LabelUtils.Equals));
        Assert.assertTrue(matchExp.getValues().equals(vals));
    }
}
