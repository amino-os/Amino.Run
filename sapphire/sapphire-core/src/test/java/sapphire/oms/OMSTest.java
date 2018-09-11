package sapphire.oms;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.addHost;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.SapphireUtils.dummyRegistry;
import static sapphire.common.SapphireUtils.getHostOnOmsKernelServerManager;
import static sapphire.common.SapphireUtils.getOmsSapphireInstance;
import static sapphire.common.SapphireUtils.startSpiedKernelServer;
import static sapphire.common.SapphireUtils.startSpiedOms;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.SO;
import sapphire.app.stubs.SO_Stub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelObject;
import sapphire.kernel.server.KernelObjectManager;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.runtime.Sapphire;

/** OMS API test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class OMSTest {
    DefaultSapphirePolicy.DefaultClientPolicy client;
    DefaultSapphirePolicy.DefaultServerPolicy server1;
    DefaultSapphirePolicy.DefaultServerPolicy server2;
    DefaultSapphirePolicy.DefaultServerPolicy server3;
    DefaultSapphirePolicy.DefaultGroupPolicy group;
    SO_Stub soStub;
    SO so;
    OMSServer spiedOms;

    @Rule public ExpectedException thrown = ExpectedException.none();

    public static class Group_Stub extends DefaultSapphirePolicy.DefaultGroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;

        public Group_Stub(sapphire.kernel.common.KernelOID oid) {
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
    }

    public static class Server_Stub extends DefaultSapphirePolicy.DefaultServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;

        public Server_Stub(KernelOID oid) {
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
    }

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(LocateRegistry.class);
        when(LocateRegistry.getRegistry(anyString(), anyInt())).thenReturn(dummyRegistry);

        // create a spied oms instance
        OMSServerImpl spiedOms = startSpiedOms();
        KernelServerImpl.oms = spiedOms;
        this.spiedOms = spiedOms;

        int kernelPort1 = 10001;
        int kernelPort2 = 10002;
        int kernelPort3 = 10003;

        // create a spied kernel server instance
        final KernelServerImpl spiedKs1 = startSpiedKernelServer(spiedOms, kernelPort1, "IND");
        final KernelServerImpl spiedKs2 = startSpiedKernelServer(spiedOms, kernelPort2, "IND");
        final KernelServerImpl spiedKs3 = startSpiedKernelServer(spiedOms, kernelPort3, "IND");

        // Set this instance of kernel server as local kernel server
        GlobalKernelReferences.nodeServer = spiedKs1;

        /* Add all the hosts to the kernel client of local kernel server instance so that every call
        becomes local invocation */
        addHost(spiedKs2);
        addHost(spiedKs3);

        // Stub the static factory create method to pass our test stub class name
        mockStatic(
                KernelObjectFactory.class,
                new Answer<Object>() {
                    int i = 0;

                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (invocation.getMethod().getName().equals("createStub")) {
                            Class<?> policy = (Class) invocation.getArguments()[0];
                            if (policy.getName().contains("Server")) {
                                invocation.getArguments()[0] = Server_Stub.class;
                            } else {
                                assert (policy.getName().contains("Group"));
                                invocation.getArguments()[0] = Group_Stub.class;
                            }

                            return invocation.callRealMethod();
                        } else if (!(invocation.getMethod().getName().equals("create"))) {
                            assert (invocation.getMethod().getName().equals("delete"));
                            KernelOID oid = (KernelOID) invocation.getArguments()[0];
                            InetSocketAddress host = spiedOms.lookupKernelObject(oid);

                            KernelServer ks = null;
                            if (host.toString().contains(String.valueOf(kernelPort1))) {
                                ks = spiedKs1;
                            } else if (host.toString().contains(String.valueOf(kernelPort2))) {
                                ks = spiedKs2;
                            } else if (host.toString().contains(String.valueOf(kernelPort3))) {
                                ks = spiedKs3;
                            }

                            KernelServerImpl temp = GlobalKernelReferences.nodeServer;
                            GlobalKernelReferences.nodeServer = (KernelServerImpl) ks;
                            Object ret = invocation.callRealMethod();
                            GlobalKernelReferences.nodeServer = temp;
                            return ret;
                        }

                        KernelObjectStub stub = null;
                        KernelObjectStub spiedStub = null;
                        String policyObjectName = (String) invocation.getArguments()[0];
                        if (policyObjectName.contains("Server")) {
                            invocation.getArguments()[0] = Server_Stub.class.getName();
                            ++i;
                            stub = (KernelObjectStub) invocation.callRealMethod();
                            spiedStub = spy(stub);
                            if (1 == i) {
                                server1 = (DefaultSapphirePolicy.DefaultServerPolicy) spiedStub;
                            } else if (2 == i) {
                                server2 = (DefaultSapphirePolicy.DefaultServerPolicy) spiedStub;
                            } else if (3 == i) {
                                server3 = (DefaultSapphirePolicy.DefaultServerPolicy) spiedStub;
                            }

                        } else if (policyObjectName.contains("Group")) {
                            invocation.getArguments()[0] = Group_Stub.class.getName();
                            stub = (KernelObjectStub) invocation.callRealMethod();
                            group =
                                    (DefaultSapphirePolicy.DefaultGroupPolicy)
                                            (spiedStub = spy(stub));
                        }

                        KernelServer ks = null;
                        if (stub.$__getHostname()
                                .toString()
                                .contains(String.valueOf(kernelPort1))) {
                            ks = spiedKs1;
                        } else if (stub.$__getHostname()
                                .toString()
                                .contains(String.valueOf(kernelPort2))) {
                            ks = spiedKs2;
                        } else if (stub.$__getHostname()
                                .toString()
                                .contains(String.valueOf(kernelPort3))) {
                            ks = spiedKs3;
                        }

                        /* set this spied stub itself as kernel object so that we can verify
                        all the operations in test cases */
                        KernelObjectManager objMgr =
                                (KernelObjectManager)
                                        extractFieldValueOnInstance(ks, "objectManager");
                        objMgr.addObject(stub.$__getKernelOID(), new KernelObject(spiedStub));

                        return spiedStub;
                    }
                });

        mockStatic(Sapphire.class);
        PowerMockito.mockStatic(
                Sapphire.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (!(invocation.getMethod().getName().equals("getPolicyStub")))
                            return invocation.callRealMethod();

                        if (invocation.getArguments().length == 2) {
                            KernelOID oid = (KernelOID) invocation.getArguments()[1];
                            KernelServer ks =
                                    getHostOnOmsKernelServerManager(
                                            spiedOms, spiedOms.lookupKernelObject(oid));
                            return ((KernelServerImpl) ks).getObject(oid);
                        }
                        return invocation.callRealMethod();
                    }
                });

        SapphireObjectID sapphireObjId = spiedOms.createSapphireObject("sapphire.app.SO");
        soStub = (SO_Stub) spiedOms.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
        so = ((SO) (server1.sapphire_getAppObject().getObject()));
    }

    @Test
    public void acquireSapphireObjectStubSuccessTest() throws Exception {
        SO_Stub appObjstub = (SO_Stub) spiedOms.acquireSapphireObjectStub(group.getSapphireObjId());
        assertEquals(
                1, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(10));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(10), so.getI());
    }

    @Test
    public void attachAndDetactSapphireObjectSuccessTest() throws Exception {
        spiedOms.setSapphireObjectName(group.getSapphireObjId(), "MySapphireObject");

        SO_Stub appObjstub = (SO_Stub) spiedOms.attachToSapphireObject("MySapphireObject");

        /* Reference count must become 2. Once user created it and other attached to it */
        assertEquals(
                2, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(100));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(100), so.getI());

        /* Reference count must become 1(decrement by 1) upon detach */
        spiedOms.detachFromSapphireObject("MySapphireObject");
        assertEquals(
                1, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());
    }

    @After
    public void tearDown() throws Exception {
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
