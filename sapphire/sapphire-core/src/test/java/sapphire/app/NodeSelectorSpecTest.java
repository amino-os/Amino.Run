package sapphire.app;

import org.junit.Test;

public class NodeSelectorSpecTest {

    @Test
    public void testNullMatchLabels() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.setMatchLabels(null);
    }

    @Test
    public void testaddMatchLabelsIteml() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addMatchLabelsItem(null, null);
    }

    @Test
    public void testsetMatchExpressions() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.setMatchExpressions(null);
    }

    @Test
    public void testaddMatchExpressionsItem() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addMatchExpressionsItem(null);
    }
}
