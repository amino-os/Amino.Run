package sapphire.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.policy.SapphirePolicyUpcalls.SapphirePolicyConfig;

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
import sapphire.policy.*;
import sapphire.policy.dht.DHTPolicy;

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
    private Map<String, Map<String, SapphirePolicyConfig>> configMaps;
    private Map<String, SapphirePolicyConfig> configMap;

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

    public static class DHTGroup_Stub extends DHTPolicy.DHTGroupPolicy implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public DHTGroup_Stub(sapphire.kernel.common.KernelOID oid) {
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

    public static class DHTServer_Stub extends DHTPolicy.DHTServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public DHTServer_Stub(KernelOID oid) {
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

        public void sapphire_pin(
                SapphirePolicy.SapphireServerPolicy sapphireServerPolicy, String region) {
            return;
        }

        /* This function is added here just to generate the stub for this function in all Policies server policy */
        public void sapphire_pin_to_server(
                SapphirePolicy.SapphireServerPolicy sapphireServerPolicy,
                InetSocketAddress server) {
            return;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.serversInSameRegion = false;

        HashMap<String, Class> groupMap = new HashMap<String, Class>();
        HashMap<String, Class> serverMap = new HashMap<String, Class>();
        groupMap.put("DHTPolicy", SapphireMultiPolicyChainTest.DHTGroup_Stub.class);
        groupMap.put("DefaultSapphirePolicy", SapphireMultiPolicyChainTest.DefaultGroup_Stub.class);
        serverMap.put("DHTPolicy", SapphireMultiPolicyChainTest.DHTServer_Stub.class);
        serverMap.put(
                "DefaultSapphirePolicy", SapphireMultiPolicyChainTest.DefaultServer_Stub.class);

        spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.app.SO")
                        .addDMSpec(DMSpec.newBuilder().setName(DHTPolicy.class.getName()).create())
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(DefaultSapphirePolicy.class.getName())
                                        .create())
                        .create();

        configMaps = Utils.fromDMSpecListToConfigMap(spec.getDmList());
        configMap = configMaps.get(DHTPolicy.class.getName());

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

        policyNameChain.add(new SapphirePolicyContainer("sapphire.policy.dht.DHTPolicy", null));
        List<SapphirePolicyContainer> policyList =
                Sapphire.createPolicy(
                        sapphireObjId,
                        spec,
                        configMap,
                        policyNameChain,
                        processedPolicies,
                        previousServerPolicy,
                        previousServerPolicyStub,
                        "",
                        null);

        assertEquals(1, policyList.size());
    }

    @Ignore
    @Test
    public void testCreatePolicyTwoPolicies() throws Exception {
        SapphirePolicy.SapphireServerPolicy previousServerPolicy = null;
        KernelObjectStub previousServerPolicyStub = null;
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<SapphirePolicyContainer>();

        /* Register for a sapphire object Id from OMS */
        SapphireObjectID sapphireObjId = spiedOms.registerSapphireObject();

        policyNameChain.add(new SapphirePolicyContainer("sapphire.policy.dht.DHTPolicy", null));
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
                        "",
                        null);

        assertEquals(2, policyList.size());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Deleting SapphireObject: " + group.getSapphireObjId());
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
