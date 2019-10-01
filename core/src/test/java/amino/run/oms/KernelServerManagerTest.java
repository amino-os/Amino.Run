package amino.run.oms;

import static amino.run.kernel.server.KernelServerImpl.REGION_KEY;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import amino.run.app.NodeSelectorSpec;
import amino.run.app.NodeSelectorTerm;
import amino.run.app.Operator;
import amino.run.app.Requirement;
import amino.run.kernel.common.ServerInfo;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest(KernelServerManager.class)
@RunWith(PowerMockRunner.class)
public class KernelServerManagerTest {
    private static final String LABEL1_PREFIX = "label1_";
    private static final String LABEL2_PREFIX = "label2_";
    private static final String NON_EXISTENT_LABEL = "non_existent_label";
    private int numOfServers = 10;
    private KernelServerManager manager;
    private ArrayList<ResettableTimer> kernelServerTimers = new ArrayList<ResettableTimer>();
    static final long KS_HEARTBEAT_PERIOD = OMSServer.KS_HEARTBEAT_TIMEOUT / 3;

    @Before
    public void setup() throws Exception {
        manager = new KernelServerManager();
        registerServers(manager, numOfServers);
    }

    @After
    public void tearDown() {
        for (ResettableTimer timer : kernelServerTimers) {
            timer.cancel();
        }
    }

    @Test
    public void testWithEqualLabel() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(LABEL1_PREFIX + "1", Operator.Equal, LABEL1_PREFIX + "1");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    @Test
    public void testWithEqualLabelFailure() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(LABEL1_PREFIX + "1", Operator.Equal, LABEL1_PREFIX + "2");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testWithInLabel() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(
                        LABEL1_PREFIX + "1", Operator.In, LABEL1_PREFIX + "1", LABEL1_PREFIX + "2");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    @Test
    public void testWithInLabelFailure() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(LABEL1_PREFIX + "1", Operator.In, LABEL1_PREFIX + "2");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testWithNotInLabel() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(LABEL1_PREFIX + "1", Operator.NotIn, LABEL1_PREFIX + "2");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    @Test
    public void testWithNotInLabelFailure() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(LABEL1_PREFIX + "1", Operator.NotIn, LABEL1_PREFIX + "1");
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testWithExistLabel() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(LABEL1_PREFIX + "1", Operator.Exists, NON_EXISTENT_LABEL);
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    @Test
    public void testWithExistLabelFailure() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(NON_EXISTENT_LABEL, Operator.Exists, NON_EXISTENT_LABEL);
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    // scenario to test multiple requirement in same term.
    // Kernel server should meet all requirements to get selected
    @Test
    public void testMultiRequirementInTerm() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(
                        LABEL1_PREFIX + "1", Operator.Equal, LABEL1_PREFIX + "1")); // requirement 1
        term.addMatchRequirements(
                getRequirement(
                        LABEL1_PREFIX + "1",
                        Operator.Equal,
                        LABEL1_PREFIX + "1")); // requirement 2, will not meet any server
        spec.addNodeSelectorTerms(term);
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    // scenario to test multiple requirement in same term.
    // Kernel server should meet all requirements to get selected
    // Here Any kernel server will not meet requirement 2
    @Test
    public void testMultiRequirementInTermFailure() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(
                        LABEL1_PREFIX + "1", Operator.Equal, LABEL1_PREFIX + "1")); // requirement 1
        term.addMatchRequirements(
                getRequirement(
                        NON_EXISTENT_LABEL,
                        Operator.Equal,
                        NON_EXISTENT_LABEL)); // requirement 2, will not meet any server
        spec.addNodeSelectorTerms(term);
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testNonExistingLabel() {
        NodeSelectorSpec spec =
                getNodeSelectorSpec(NON_EXISTENT_LABEL, Operator.Equal, NON_EXISTENT_LABEL);
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    // scenario to test with multiple terms in node selection spec
    // if a kernel server meet any term, it will get selected
    @Test
    public void testMultiTerm() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(LABEL1_PREFIX + "1", Operator.Equal, LABEL1_PREFIX + "1")); // term 1
        spec.addNodeSelectorTerms(term);
        term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(NON_EXISTENT_LABEL, Operator.Equal, NON_EXISTENT_LABEL));
        spec.addNodeSelectorTerms(term); // term 2
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    @Test
    public void testMultiTermFailure() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(NON_EXISTENT_LABEL, Operator.Equal, NON_EXISTENT_LABEL)); // term 1
        spec.addNodeSelectorTerms(term);
        term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(NON_EXISTENT_LABEL, Operator.Equal, NON_EXISTENT_LABEL));
        spec.addNodeSelectorTerms(term); // term 2
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testEmptyNodeSelectionTerm() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(numOfServers, result.size());
    }

    @Test
    public void testNullNodeSelectorSpec() {
        List<InetSocketAddress> result = manager.getServers(null);
        Assert.assertEquals(numOfServers, result.size());
    }

    @Test
    public void testAndRequirementORRequirementSuccess() {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(LABEL1_PREFIX + "1", Operator.Equal, LABEL1_PREFIX + "1")); // term 1
        term.addMatchRequirements(
                getRequirement(LABEL2_PREFIX + "1", Operator.Equal, LABEL2_PREFIX + "1"));
        spec.addNodeSelectorTerms(term); // term1
        term = new NodeSelectorTerm();
        term.addMatchRequirements(
                getRequirement(LABEL1_PREFIX + "1", Operator.Equal, LABEL1_PREFIX + "1"));
        term.addMatchRequirements(
                getRequirement(NON_EXISTENT_LABEL, Operator.Equal, NON_EXISTENT_LABEL));
        spec.addNodeSelectorTerms(term); // term 2

        List<InetSocketAddress> result = manager.getServers(spec);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getPort());
    }

    private void registerServers(final KernelServerManager manager, int numOfServers)
            throws Exception {
        ResettableTimer timer;
        mockStatic(LocateRegistry.class);
        Registry registry =
                new Registry() {
                    @Override
                    public Remote lookup(String name)
                            throws RemoteException, NotBoundException, AccessException {
                        return null;
                    }

                    @Override
                    public void bind(String name, Remote obj)
                            throws RemoteException, AlreadyBoundException, AccessException {}

                    @Override
                    public void unbind(String name)
                            throws RemoteException, NotBoundException, AccessException {}

                    @Override
                    public void rebind(String name, Remote obj)
                            throws RemoteException, AccessException {}

                    @Override
                    public String[] list() throws RemoteException, AccessException {
                        return new String[0];
                    }
                };
        for (int i = 0; i < numOfServers; i++) {
            final ServerInfo s =
                    new ServerInfo(
                            new InetSocketAddress(i), Runtime.getRuntime().availableProcessors());
            HashMap labels = new HashMap();
            labels.put(LABEL1_PREFIX + i, LABEL1_PREFIX + i);
            labels.put(LABEL2_PREFIX + i, LABEL2_PREFIX + i);
            labels.put(REGION_KEY, "region_" + i);
            s.addLabels(labels);
            when(LocateRegistry.getRegistry("0.0.0.0", i)).thenReturn(registry);
            manager.registerKernelServer(s);
            timer =
                    new ResettableTimer(
                            new TimerTask() {
                                public void run() {
                                    try {
                                        manager.receiveHeartBeat(s);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            KS_HEARTBEAT_PERIOD);
            timer.start();
            kernelServerTimers.add(timer);
        }
    }

    private NodeSelectorTerm getNodeSelectorTerm(String key, Operator operator, String... labels) {
        NodeSelectorTerm term = new NodeSelectorTerm();
        term.addMatchRequirements(getRequirement(key, operator, labels));
        return term;
    }

    private Requirement getRequirement(String key, Operator operator, String... labels) {
        if (Operator.Exists.equals(operator)) {
            return new Requirement(key, operator, null);
        }
        return new Requirement(key, operator, Arrays.asList(labels));
    }

    private NodeSelectorSpec getNodeSelectorSpec(String key, Operator operator, String... labels) {
        NodeSelectorSpec spec = new NodeSelectorSpec();
        spec.addNodeSelectorTerms(getNodeSelectorTerm(key, operator, labels));
        return spec;
    }
}
