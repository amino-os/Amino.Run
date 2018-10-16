package sapphire.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static sapphire.common.SapphireUtils.deleteSapphireObject;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SO;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.*;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicyContainer;
import sapphire.policy.SapphirePolicyUpcalls;
import sapphire.policy.dht.DHTPolicy2;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class SapphireMultiPolicyChainTest extends MultiPolicyChainBaseTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    public static class DefaultSO extends SO {}

    private SapphireObjectSpec spec;
    private Map<String, Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig>> configMaps;
    private Map<String, SapphirePolicyUpcalls.SapphirePolicyConfig> configMap;

    public static class DefaultGroup_Stub extends DefaultSapphirePolicy.DefaultGroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public DefaultGroup_Stub(sapphire.kernel.common.KernelOID oid) {
            this.$__oid = oid;
        }

        public sapphire.kernel.common.KernelOID $__getKernelOID() {
            return this.$__oid;
        }

        public java.net.InetSocketAddress $__getHostname() {
            return this.$__hostname;
        }

        public void $__updateHostname(java.net.InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class DefaultServer_Stub extends DefaultSapphirePolicy.DefaultServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public DefaultServer_Stub(KernelOID oid) {
            this.oid = oid;
            this.$__oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return $__oid;
        }

        public InetSocketAddress $__getHostname() {
            return $__hostname;
        }

        public void $__updateHostname(InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class DHT2Group_Stub extends DHTPolicy2.DHTGroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public DHT2Group_Stub(sapphire.kernel.common.KernelOID oid) {
            this.$__oid = oid;
        }

        public sapphire.kernel.common.KernelOID $__getKernelOID() {
            return this.$__oid;
        }

        public java.net.InetSocketAddress $__getHostname() {
            return this.$__hostname;
        }

        public void $__updateHostname(java.net.InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class DHT2Server_Stub extends DHTPolicy2.DHTServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public DHT2Server_Stub(KernelOID oid) {
            this.oid = oid;
            this.$__oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return $__oid;
        }

        public InetSocketAddress $__getHostname() {
            return $__hostname;
        }

        public void $__updateHostname(InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.serversInSameRegion = false;

        HashMap<String, Class> groupMap = new HashMap<String, Class>();
        HashMap<String, Class> serverMap = new HashMap<String, Class>();
        groupMap.put("DHTPolicy2", SapphireMultiPolicyChainTest.DHT2Group_Stub.class);
        groupMap.put("DefaultSapphirePolicy", SapphireMultiPolicyChainTest.DefaultGroup_Stub.class);
        serverMap.put("DHTPolicy2", SapphireMultiPolicyChainTest.DHT2Server_Stub.class);
        serverMap.put(
                "DefaultSapphirePolicy", SapphireMultiPolicyChainTest.DefaultServer_Stub.class);

        spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.app.SO")
                        .addDMSpec(DMSpec.newBuilder().setName(DHTPolicy2.class.getName()).create())
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(DefaultSapphirePolicy.class.getName())
                                        .create())
                        .create();

        configMaps = Utils.fromDMSpecListToConfigMap(spec.getDmList());
        configMap = configMaps.get(DHTPolicy2.class.getName());

        super.setUpMultiDM(spec, groupMap, serverMap);
    }

    @Test
    public void testNew_() throws Exception {
        Object temp = Sapphire.new_(DefaultSO.class);
        assertNotEquals(null, temp);
    }

    @Test
    public void testCreatePolicy() throws Exception {
        SapphirePolicy.SapphireServerPolicy previousServerPolicy = null;
        KernelObjectStub previousServerPolicyStub = null;
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<SapphirePolicyContainer>();

        /* Register for a sapphire object Id from OMS */
        SapphireObjectID sapphireObjId = spiedOms.registerSapphireObject();

        policyNameChain.add(new SapphirePolicyContainer("sapphire.policy.dht.DHTPolicy2", null));
        List<SapphirePolicyContainer> policyList =
                Sapphire.createPolicy(
                        sapphireObjId,
                        spec,
                        configMap,
                        policyNameChain,
                        processedPolicies,
                        previousServerPolicy,
                        previousServerPolicyStub,
                        null);

        assertEquals(1, policyList.size());
    }

    @Test
    public void testCreatePolicyTwoPolicies() throws Exception {
        SapphirePolicy.SapphireServerPolicy previousServerPolicy = null;
        KernelObjectStub previousServerPolicyStub = null;
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<SapphirePolicyContainer>();

        /* Register for a sapphire object Id from OMS */
        SapphireObjectID sapphireObjId = spiedOms.registerSapphireObject();

        policyNameChain.add(new SapphirePolicyContainer("sapphire.policy.dht.DHTPolicy2", null));
        policyNameChain.add(
                new SapphirePolicyContainer("sapphire.policy.DefaultSapphirePolicy", null));

        List<SapphirePolicyContainer> policyList =
                Sapphire.createPolicy(
                        sapphireObjId,
                        spec,
                        configMap,
                        policyNameChain,
                        processedPolicies,
                        previousServerPolicy,
                        previousServerPolicyStub,
                        null);

        assertEquals(2, policyList.size());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Deleting SapphireObject: " + group.getSapphireObjId());
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
