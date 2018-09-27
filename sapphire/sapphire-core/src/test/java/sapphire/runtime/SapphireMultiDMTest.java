package sapphire.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static sapphire.common.SapphireUtils.deleteSapphireObject;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.SO;
import sapphire.common.AppObject;
import sapphire.common.MultiDMBaseTest;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicyContainer;
import sapphire.policy.SapphirePolicyContainerImpl;
import sapphire.policy.dht.DHTPolicy2;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class SapphireMultiDMTest extends MultiDMBaseTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    @SapphireConfiguration(
            Policies = "sapphire.policy.dht.DHTPolicy2, sapphire.policy.DefaultSapphirePolicy")
    public static class DefaultSO extends SO {}

    public static class DefaultGroup_Stub extends DefaultSapphirePolicy.DefaultGroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;
        AppObject $__appObject = null;
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

        public int $__getLastSeenTick() {
            return $__lastSeenTick;
        }

        public void $__setLastSeenTick(int lastSeenTick) {
            this.$__lastSeenTick = lastSeenTick;
        }

        public AppObject $__getAppObject() {
            return $__appObject;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class DefaultServer_Stub extends DefaultSapphirePolicy.DefaultServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;
        AppObject $__appObject = null;
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

        public int $__getLastSeenTick() {
            return $__lastSeenTick;
        }

        public void $__setLastSeenTick(int lastSeenTick) {
            this.$__lastSeenTick = lastSeenTick;
        }

        public AppObject $__getAppObject() {
            return $__appObject;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class DHT2Group_Stub extends DHTPolicy2.DHTGroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;
        AppObject $__appObject = null;
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

        public int $__getLastSeenTick() {
            return $__lastSeenTick;
        }

        public void $__setLastSeenTick(int lastSeenTick) {
            this.$__lastSeenTick = lastSeenTick;
        }

        public AppObject $__getAppObject() {
            return $__appObject;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class DHT2Server_Stub extends DHTPolicy2.DHTServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;
        AppObject $__appObject = null;
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

        public int $__getLastSeenTick() {
            return $__lastSeenTick;
        }

        public void $__setLastSeenTick(int lastSeenTick) {
            this.$__lastSeenTick = lastSeenTick;
        }

        public AppObject $__getAppObject() {
            return $__appObject;
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
        groupMap.put("DHTPolicy2", SapphireMultiDMTest.DHT2Group_Stub.class);
        groupMap.put("DefaultSapphirePolicy", SapphireMultiDMTest.DefaultGroup_Stub.class);
        serverMap.put("DHTPolicy2", SapphireMultiDMTest.DHT2Server_Stub.class);
        serverMap.put("DefaultSapphirePolicy", SapphireMultiDMTest.DefaultServer_Stub.class);
        super.setUpMultiDM(groupMap, serverMap);
    }

    @Test
    public void testNew_() throws Exception {
        Object temp = Sapphire.new_(DefaultSO.class, null);
        assertNotEquals(null, temp);
    }

    @Test
    public void testCreatePolicy() throws Exception {
        SapphirePolicy.SapphireServerPolicy previousServerPolicy = null;
        KernelObjectStub previousServerPolicyStub = null;
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<SapphirePolicyContainer>();

        policyNameChain.add(
                new SapphirePolicyContainerImpl("sapphire.policy.dht.DHTPolicy2", null));
        List<SapphirePolicyContainer> policyList =
                Sapphire.createPolicy(
                        DefaultSO.class,
                        null,
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

        policyNameChain.add(
                new SapphirePolicyContainerImpl("sapphire.policy.dht.DHTPolicy2", null));
        policyNameChain.add(
                new SapphirePolicyContainerImpl("sapphire.policy.DefaultSapphirePolicy", null));

        List<SapphirePolicyContainer> policyList =
                Sapphire.createPolicy(
                        DefaultSO.class,
                        null,
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
