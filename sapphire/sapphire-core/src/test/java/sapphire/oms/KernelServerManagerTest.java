package sapphire.oms;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.app.NodeSelectorSpec;
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
