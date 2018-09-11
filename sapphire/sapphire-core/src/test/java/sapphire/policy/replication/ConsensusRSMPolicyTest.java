package sapphire.policy.replication;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.addHost;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.SapphireUtils.dummyRegistry;
import static sapphire.common.SapphireUtils.getHostOnOmsKernelServerManager;
import static sapphire.common.SapphireUtils.startSpiedKernelServer;
import static sapphire.common.SapphireUtils.startSpiedOms;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.policy.util.consensus.raft.ServerTest.makeFollower;
import static sapphire.policy.util.consensus.raft.ServerTest.makeLeader;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.SO;
import sapphire.app.stubs.SO_Stub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.common.Utils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelObject;
import sapphire.kernel.server.KernelObjectManager;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.consensus.raft.LeaderException;
import sapphire.runtime.Sapphire;

/** Created by terryz on 4/9/18. */
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class ConsensusRSMPolicyTest {
    DefaultSapphirePolicy.DefaultClientPolicy client;
    DefaultSapphirePolicy.DefaultServerPolicy server1;
    DefaultSapphirePolicy.DefaultServerPolicy server2;
    DefaultSapphirePolicy.DefaultServerPolicy server3;
    DefaultSapphirePolicy.DefaultGroupPolicy group;
    sapphire.policy.util.consensus.raft.Server raftServer1;
    sapphire.policy.util.consensus.raft.Server raftServer2;
    sapphire.policy.util.consensus.raft.Server raftServer3;
    SO_Stub soStub;
    SO so;
    OMSServer spiedOms;

    public static class Group_Stub extends ConsensusRSMPolicy.GroupPolicy
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

    public static class Server_Stub extends ConsensusRSMPolicy.ServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        int $__lastSeenTick = 0;

        public Server_Stub(KernelOID oid) {
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
        KernelServerImpl spiedKs1 = startSpiedKernelServer(spiedOms, kernelPort1, "IND");
        KernelServerImpl spiedKs2 = startSpiedKernelServer(spiedOms, kernelPort2, "CHN");
        KernelServerImpl spiedKs3 = startSpiedKernelServer(spiedOms, kernelPort3, "USA");

        // Set this instance of kernel server as local kernel server
        GlobalKernelReferences.nodeServer = spiedKs1;

        // Add all the hosts to the kernel client of local kernel server instance
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
                                invocation.getArguments()[0] =
                                        ConsensusRSMPolicyTest.Server_Stub.class;
                            } else {
                                assert (policy.getName().contains("Group"));
                                invocation.getArguments()[0] =
                                        ConsensusRSMPolicyTest.Group_Stub.class;
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
                            invocation.getArguments()[0] =
                                    ConsensusRSMPolicyTest.Server_Stub.class.getName();
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
                            invocation.getArguments()[0] =
                                    ConsensusRSMPolicyTest.Group_Stub.class.getName();
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
        so = (SO_Stub) spiedOms.acquireSapphireObjectStub(sapphireObjId);

        client = spy(new ConsensusRSMPolicy.ClientPolicy());
        client.onCreate(group, null);

        /* Ensure the order of servers is same as in server list in group */
        ArrayList<SapphirePolicy.SapphireServerPolicy> servers =
                this.client.getGroup().getServers();
        this.server1 = (ConsensusRSMPolicy.ServerPolicy) servers.get(0);
        this.server2 = (ConsensusRSMPolicy.ServerPolicy) servers.get(1);
        this.server3 = (ConsensusRSMPolicy.ServerPolicy) servers.get(2);

        client.setServer(server1);
        soStub = new SO_Stub();
        soStub.$__initialize(client);

        /* Make server3 as raft leader */
        raftServer1 =
                (sapphire.policy.util.consensus.raft.Server)
                        extractFieldValueOnInstance(server1, "raftServer");
        raftServer2 =
                (sapphire.policy.util.consensus.raft.Server)
                        extractFieldValueOnInstance(server2, "raftServer");
        raftServer3 =
                (sapphire.policy.util.consensus.raft.Server)
                        extractFieldValueOnInstance(server3, "raftServer");
        makeFollower(raftServer1);
        makeFollower(raftServer2);
        makeLeader(raftServer3);
    }

    @Test
    public void testSerialization() throws Exception {
        ConsensusRSMPolicy.ClientPolicy client = new ConsensusRSMPolicy.ClientPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(client));

        ConsensusRSMPolicy.GroupPolicy group = new ConsensusRSMPolicy.GroupPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(group));

        ConsensusRSMPolicy.ServerPolicy server = new ConsensusRSMPolicy.ServerPolicy();
        // server.onCreate(group, new Annotation[] {});
        assertNotNull(Utils.ObjectCloner.deepCopy(server));
    }

    @Test
    public void applyToStateMachine() throws Exception {
        String method = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        ConsensusRSMPolicy.RPC rpc = new ConsensusRSMPolicy.RPC(method, params);
        ConsensusRSMPolicy.ServerPolicy server3 = (ConsensusRSMPolicy.ServerPolicy) this.server3;
        server3.applyToStateMachine(rpc);
        sleep(1000);
        verify(server3, times(1)).apply(Matchers.anyObject());
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void omsNotAvailable() throws Exception {
        when(this.group.sapphire_getRegions()).thenThrow(new RemoteException());
        thrown.expect(Error.class);
        this.group.onCreate(this.server1, new Annotation[] {});
    }

    /**
     * Try onRPC on the first server, if this is not the leader throw LeaderException. Retry rpc on
     * the next server, if leader not elected throw RemoteException.
     */
    @Test
    public void onRPCWithoutLeader() throws Exception {
        String method = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(new LeaderException("leaderException", null))
                .when(this.server1)
                .onRPC(method, params);
        thrown.expect(RemoteException.class);
        thrown.expectMessage("Raft leader is not elected");
        this.client.onRPC(method, params);
    }

    /**
     * Try onRPC on the non leader server, if this is not the leader throw LeaderException. Retry
     * rpc on the leader server.
     */
    @Test
    public void onRPCWithLeader() throws Exception {
        String method = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        client.onRPC(method, params);
    }

    /**
     * If onRPC fails on the server with remote exception, get servers from group and find a
     * responding server. Try with next server until you find a responding server.
     */
    @Test
    public void clientOnRPC() throws Exception {
        String method = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(RemoteException.class).when(this.server2).onRPC(method, params);
        client.onRPC(method, params);
    }

    /** If onRPC fails to happen on any of the servers, throw RemoteException. */
    @Test
    public void onRPCServersUnreachable() throws Exception {
        String method = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(RemoteException.class).when(this.server2).onRPC(method, params);
        doThrow(RemoteException.class).when(this.server3).onRPC(method, params);
        thrown.expect(RemoteException.class);
        thrown.expectMessage("Failed to connect atleast one server");
        this.client.onRPC(method, params);
    }

    /**
     * If onRPC fails on the leader, get servers from group and find a responding server. If leader
     * still not elected, throw RemoteException.
     */
    @Test
    public void onRPCToFollowerWithoutLeader() throws Exception {
        String method = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(new LeaderException("LeaderException", null))
                .when(this.server2)
                .onRPC(method, params);
        thrown.expect(RemoteException.class);
        thrown.expectMessage("Raft leader is not elected");
        this.client.onRPC(method, params);
    }

    /**
     * If onRPC fails on the leader, get servers from group and find a responding server. Store this
     * server as reachable and use it for future rpc.
     */
    @Test
    public void onRPCToFollowerWithLeader() throws Exception {
        String method = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> params = new ArrayList<Object>();
        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(
                        new LeaderException(
                                "LeaderException", (ConsensusRSMPolicy.ServerPolicy) this.server3))
                .when(this.server2)
                .onRPC(method, params);
        this.client.onRPC(method, params);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("in teardown.....");
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
