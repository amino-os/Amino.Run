package sapphire.app;

import org.junit.Test;

public class NodeSelectorSpecTest {

    @Test
    public void testAddNullOrLabels() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addOrLabels(null);
    }

    @Test
    public void testAddNullOrLabel() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addOrLabel(null);
    }

    @Test
    public void testAddNullAndLabels() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addAndLabels(null);
    }

    @Test
    public void testAddNullAndLabel() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addAndLabel(null);
    }
}
