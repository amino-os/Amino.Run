package sapphire.app;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import sapphire.common.Utils;

public class NodeSelectorRequirementTest {
    @Test
    public void testNullsetKey() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        spec.setKey(null);
    }

    @Test
    public void testsetOperator() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        spec.setOperator(null);
    }

    @Test
    public void testsetValues() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        spec.setValues(null);
    }

    @Test
    public void testaddValuesItem() {
        NodeSelectorRequirement spec = new NodeSelectorRequirement();
        spec.addValuesItem(null);
    }

    @Test
    public void testNodeSelectorRequirement() {
        String key = "key1";
        String[] values = {"val1", "val2", "val3"};
        List<String> vals = Arrays.asList(values);
        NodeSelectorRequirement matchExp = new NodeSelectorRequirement();
        matchExp.setKey(key);
        matchExp.setOperator(Utils.Equals);
        matchExp.setValues(vals);

        Assert.assertTrue(matchExp.getKey().equals(key));
        Assert.assertTrue(matchExp.getOperator().equals(Utils.Equals));
        Assert.assertTrue(matchExp.getValues().equals(vals));
    }
}
