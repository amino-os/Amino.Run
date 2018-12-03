package sapphire.app;

import org.junit.Assert;
import org.junit.Test;

public class NodeSelectorSpecTest {

    @Test
    public void testNullMatchLabels() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        try {
            spec.setMatchLabels(null);
        } catch (Exception e) {
            Assert.fail("Should not assert");
        }
    }

    @Test
    public void testaddMatchLabelsIteml() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        try {
            spec.addMatchLabelsItem(null, null);
            Assert.fail("Should not execute this because should throw Exception");
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            Assert.fail("Should not execute this because should throw IllegalArgumentException");
        }
    }

    @Test
    public void testsetMatchExpressions() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        try {
            spec.setNodeAffinity(null);
        } catch (Exception e) {
            Assert.fail("Should not assert");
        }
    }
}
