package sapphire.policy.replication;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.addHost;
import static sapphire.common.SapphireUtils.startSpiedKernelServer;
import static sapphire.common.SapphireUtils.startSpiedOms;
import static sapphire.common.UtilsTest.setFieldValueOnInstance;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.UUID;
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
import sapphire.common.AppObject;
import sapphire.common.SapphireUtils;
import sapphire.common.Utils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.consensus.raft.LeaderException;
import sapphire.policy.util.consensus.raft.Server;

/** Created by terryz on 4/9/18. */
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class ConsensusRSMPolicyTest implements Serializable {
    ConsensusRSMPolicy.ClientPolicy client;
    ConsensusRSMPolicy.ServerPolicy server1;
    ConsensusRSMPolicy.ServerPolicy server2;
    ConsensusRSMPolicy.ServerPolicy server3;
    ConsensusRSMPolicy.GroupPolicy group;

    private AppObject appObject;
    private ConsensusRSMPolicyTest so;

    public static class Server_Stub extends ConsensusRSMPolicy.ServerPolicy
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
        Server raftServer0 = mock(Server.class);
        Server raftServer1 = mock(Server.class);
        Server raftServer2 = mock(Server.class);

        Registry test =
                new Registry() {
                    @Override
                    public Remote lookup(String s)
                            throws RemoteException, NotBoundException, AccessException {
                        return null;
                    }

                    @Override
                    public void bind(String s, Remote remote)
                            throws RemoteException, AlreadyBoundException, AccessException {}

                    @Override
                    public void unbind(String s)
                            throws RemoteException, NotBoundException, AccessException {}

                    @Override
                    public void rebind(String s, Remote remote)
                            throws RemoteException, AccessException {}

                    @Override
                    public String[] list() throws RemoteException, AccessException {
                        return new String[0];
                    }
                };

        PowerMockito.mockStatic(LocateRegistry.class);
        when(LocateRegistry.getRegistry(anyString(), anyInt())).thenReturn(test);

        // create a spied oms instance
        OMSServerImpl spiedOms = startSpiedOms("ConsensusRSMPolicyTest");
        KernelServerImpl.oms = spiedOms;
        // create a spied kernel server instance
        KernelServerImpl spiedKs1 = startSpiedKernelServer(spiedOms, 10001, "IND");
        KernelServerImpl spiedKs2 = startSpiedKernelServer(spiedOms, 10002, "CHN");
        KernelServerImpl spiedKs3 = startSpiedKernelServer(spiedOms, 10003, "USA");
        // Set this instance of kernel server as local kernel server
        GlobalKernelReferences.nodeServer = spiedKs1;

        this.client = spy(ConsensusRSMPolicy.ClientPolicy.class);
        so = new ConsensusRSMPolicyTest();
        appObject = new AppObject(so);

        this.server1 =
                (ConsensusRSMPolicy.ServerPolicy)
                        spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        this.server1.$__initialize(appObject);

        this.group = spy(new ConsensusRSMPolicy.GroupPolicy());

        this.client.onCreate(this.group, new Annotation[] {});
        this.server1.onCreate(this.group, new Annotation[] {});

        // Stub the static factory create method to pass our test stub class name
        final KernelObjectStub spiedReplicaServerStub1 =
                spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        final KernelObjectStub spiedReplicaServerStub2 =
                spy(KernelObjectFactory.create(Server_Stub.class.getName()));
        mockStatic(KernelObjectFactory.class);

        PowerMockito.when(KernelObjectFactory.create(anyString()))
                .thenAnswer(
                        new Answer<KernelObjectStub>() {
                            int i = 0;

                            @Override
                            public KernelObjectStub answer(InvocationOnMock invocation)
                                    throws Throwable {
                                KernelObjectStub stub = null;
                                ++i;

                                if (1 == i) {
                                    stub = spiedReplicaServerStub1;
                                } else if (2 == i) {
                                    stub = spiedReplicaServerStub2;
                                }

                                return stub;
                            }
                        });

        // Add all the hosts to the kernel client of local kernel server instance
        addHost(spiedKs2);
        addHost(spiedKs3);

        this.group.onCreate(this.server1, new Annotation[] {});

        this.server2 = (ConsensusRSMPolicy.ServerPolicy) spiedReplicaServerStub1;
        this.server3 = (ConsensusRSMPolicy.ServerPolicy) spiedReplicaServerStub2;

        setFieldValueOnInstance(this.server1, "raftServer", raftServer0);
        setFieldValueOnInstance(this.server2, "raftServer", raftServer1);
        setFieldValueOnInstance(this.server3, "raftServer", raftServer2);

        // Update the app objects in all the stubs created
        for (SapphirePolicy.SapphireServerPolicy stub : this.group.getServers()) {
            // Should update this.server2.. and so on based on the number of server stubs created
            stub.$__initialize(appObject);
        }

        ArrayList<SapphirePolicy.SapphireServerPolicy> servers =
                this.client.getGroup().getServers();
        this.server1 = (ConsensusRSMPolicy.ServerPolicy) servers.get(0);
        this.server2 = (ConsensusRSMPolicy.ServerPolicy) servers.get(1);
        this.server3 = (ConsensusRSMPolicy.ServerPolicy) servers.get(2);
        this.client.setServer(this.server1);
    }

    @Test
    public void testSerialization() throws Exception {
        ConsensusRSMPolicy.ClientPolicy client = new ConsensusRSMPolicy.ClientPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(client));

        ConsensusRSMPolicy.GroupPolicy group = new ConsensusRSMPolicy.GroupPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(group));

        ConsensusRSMPolicy.ServerPolicy server = new ConsensusRSMPolicy.ServerPolicy();
        server.onCreate(group, new Annotation[] {});
        assertNotNull(Utils.ObjectCloner.deepCopy(server));
    }

    @Test
    public void requestVote() throws Exception {
        this.server1.requestVote(anyInt(), (UUID) anyObject(), anyInt(), anyInt());
        verify(this.server1, times(1))
                .requestVote(anyInt(), (UUID) anyObject(), anyInt(), anyInt());
    }

    @Test
    public void appendEntries() throws Exception {
        this.server1.appendEntries(
                anyInt(), (UUID) anyObject(), anyInt(), anyInt(), anyList(), anyInt());
        verify(this.server1, times(1))
                .appendEntries(
                        anyInt(), (UUID) anyObject(), anyInt(), anyInt(), anyList(), anyInt());
    }

    @Test
    public void applyToStateMachine() throws Exception {
        this.server1.applyToStateMachine(Matchers.anyObject());
        verify(this.server1, times(1)).applyToStateMachine(Matchers.anyObject());
    }

    @Test
    public void apply() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> args = new ArrayList<Object>();

        Object obj = new ConsensusRSMPolicy.RPC(methodName, args);
        this.server1.apply(obj);
        verify(this.server1, times(1)).apply(obj);

        Object obj1 = new ConsensusRSMPolicy.RPC(methodName, null);
        this.server1.apply(obj1);
        verify(this.server1, times(1)).apply(obj1);
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
        String method = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(new LeaderException("leaderException", null))
                .when(this.server1)
                .onRPC(method, params);
        thrown.expect(RemoteException.class);
        thrown.expectMessage("Raft leader is not elected");
        this.client.onRPC(method, params);
    }

    /**
     * Try onRPC on the first server, if this is not the leader throw LeaderException. Retry rpc on
     * the leader server.
     */
    @Test
    public void onRPCWithLeader() throws Exception {
        String method = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(new LeaderException("leaderException", this.server2))
                .when(this.server1)
                .onRPC(method, params);
        this.client.onRPC(method, params);
    }

    /**
     * If onRPC fails on the leader, get servers from group and find a responding server. Try with
     * next server until you find a responding server.
     */
    @Test
    public void clientOnRPC() throws Exception {
        String method = "public java.lang.String java.lang.Object.toString()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(RemoteException.class).when(this.server2).onRPC(method, params);
        this.client.onRPC(method, params);
    }

    /** If onRPC fails to happen on any of the servers, throw RemoteException. */
    @Test
    public void onRPCServersUnreachable() throws Exception {
        String method = "public java.lang.String java.lang.Object.toString()";
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
        String method = "public java.lang.String java.lang.Object.toString()";
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
        doThrow(new LeaderException("LeaderException", this.server3))
                .when(this.server2)
                .onRPC(method, params);
        this.client.onRPC(method, params);
    }
}
