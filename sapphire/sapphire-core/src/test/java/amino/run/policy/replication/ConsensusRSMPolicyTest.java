package amino.run.policy.replication;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static amino.run.policy.util.consensus.raft.Server.LEADER_HEARTBEAT_TIMEOUT;
import static amino.run.policy.util.consensus.raft.ServerTest.getOperation;
import static amino.run.policy.util.consensus.raft.ServerTest.getRaftlog;
import static amino.run.policy.util.consensus.raft.ServerTest.getTerm;
import static amino.run.policy.util.consensus.raft.ServerTest.makeCandidate;
import static amino.run.policy.util.consensus.raft.ServerTest.makeFollower;
import static amino.run.policy.util.consensus.raft.ServerTest.verifyLastApplied;
import static amino.run.policy.util.consensus.raft.ServerTest.verifyLeaderElected;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.NodeSelectorSpec;
import amino.run.common.BaseTest;
import amino.run.common.MicroServiceID;
import amino.run.common.ReplicaID;
import amino.run.common.Utils;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.policy.Policy;
import amino.run.policy.util.consensus.raft.LeaderException;
import amino.run.policy.util.consensus.raft.LogEntry;
import amino.run.policy.util.consensus.raft.Server;
import amino.run.sampleSO.SO;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/** Created by terryz on 4/9/18. */
@RunWith(PowerMockRunner.class)
public class ConsensusRSMPolicyTest extends BaseTest {
    final int SERVER_COUNT = 3;
    Server raftServer[] = new Server[SERVER_COUNT];
    SO so1, so2, so3;

    @Rule public ExpectedException thrown = ExpectedException.none();

    /**
     * This class is used by groupPolicyOnCreateFailure() only. It is used to prevent class type
     * cast exception when using spy on Consensus class. The cast exception happens when converting
     * the server instance to KernelObjectStub since spy wraps the instance with mockito class.
     */
    private static class ServerMock extends ConsensusRSMPolicy.ServerPolicy
            implements KernelObjectStub {
        @Override
        public InetSocketAddress $__getHostname() {
            return null;
        }

        @Override
        public ReplicaID getReplicaId() {
            return new ReplicaID(new MicroServiceID(UUID.randomUUID()), UUID.randomUUID());
        }

        @Override
        public void $__updateHostname(InetSocketAddress hostname) {}

        @Override
        public void $__setNextClientPolicy(Policy.ClientPolicy clientPolicy) {}
    }

    @Before
    public void setUp() throws Exception {
        this.serversInSameRegion = false;

        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .setNodeSelectorSpec(new NodeSelectorSpec())
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(ConsensusRSMPolicy.class.getName())
                                        .create())
                        .create();
        super.setUp(3, spec);

        /* Make server3 as raft leader */
        raftServer[0] = (Server) extractFieldValueOnInstance(server1, "raftServer");
        raftServer[1] = (Server) extractFieldValueOnInstance(server2, "raftServer");
        raftServer[2] = (Server) extractFieldValueOnInstance(server3, "raftServer");
        makeFollower(raftServer[0]);
        makeFollower(raftServer[1]);
        makeCandidate(raftServer[2]);

        assertTrue(raftServer[verifyLeaderElected(raftServer)] == raftServer[2]);

        so1 = ((SO) (server1.getAppObject().getObject()));
        so2 = ((SO) (server2.getAppObject().getObject()));
        so3 = ((SO) (server3.getAppObject().getObject()));
    }

    /**
     * Test whether DM objects are serializable
     *
     * @throws Exception
     */
    @Test
    public void testSerialization() throws Exception {
        ConsensusRSMPolicy.ClientPolicy client = new ConsensusRSMPolicy.ClientPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(client));

        ConsensusRSMPolicy.GroupPolicy group = new ConsensusRSMPolicy.GroupPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(group));

        ConsensusRSMPolicy.ServerPolicy server = new ConsensusRSMPolicy.ServerPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(server));
    }

    /**
     * Have 3 raft servers with leader elected. Do RPC invocation to increment a value and check if
     * they are applied on all the servers
     *
     * @throws Exception
     */
    @Test
    public void applyToStateMachine() throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();
        ConsensusRSMPolicy.RPC rpc = new ConsensusRSMPolicy.RPC(method, params);
        ConsensusRSMPolicy.ServerPolicy server3 = (ConsensusRSMPolicy.ServerPolicy) this.server3;

        server3.applyToStateMachine(rpc);

        // Verifying post applyToStateMachine, that clients have the same lastApplied
        verifyLastApplied(raftServer[2], raftServer[0]);
        verifyLastApplied(raftServer[2], raftServer[1]);

        // Verifying that all clients see the same resulting value
        assertEquals(so3.getI(), so1.getI());
        assertEquals(so3.getI(), so2.getI());

        server3.applyToStateMachine(rpc);
        // Verifying post applyToStateMachine, that clients have the same lastApplied
        verifyLastApplied(raftServer[2], raftServer[0]);
        verifyLastApplied(raftServer[2], raftServer[1]);
        // Verifying that all clients see the same resulting value
        assertEquals(so3.getI(), so1.getI());
        assertEquals(so3.getI(), so2.getI());

        /**
         * Verifying that all clients see the same event log (i.e. operation and term as seen by
         * different clients in their respective logs, is same for the same index position.)
         */
        for (int i = 0; i < getRaftlog(raftServer[2]).size(); i++) {
            LogEntry leaderEntry = (LogEntry) getRaftlog(raftServer[2]).get(i);
            LogEntry entry1 = (LogEntry) getRaftlog(raftServer[0]).get(i);
            LogEntry entry2 = (LogEntry) getRaftlog(raftServer[1]).get(i);

            // Matching Operation
            assertEquals(getOperation(leaderEntry), getOperation(entry1));
            // Matching Term
            assertEquals(getTerm(leaderEntry), getTerm(entry1));

            // Matching Operation
            assertEquals(getOperation(leaderEntry), getOperation(entry2));
            // Matching Term
            assertEquals(getTerm(leaderEntry), getTerm(entry2));
        }
    }

    /**
     * Group policy oncreate failure with remote exception
     *
     * @throws Exception
     */
    @Test
    public void groupPolicyOnCreateFailure() throws Exception {
        // ServerMock is needed because of type casting to KernelServerObject.
        // If spy(CosensusRSMPolicy.ServerPolicy.class) is used, cast fails due to mock wrapper.
        ServerMock server = new ServerMock();
        Policy.GroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        when(server.getReplicaId())
                .thenReturn(
                        new ReplicaID(new MicroServiceID(UUID.randomUUID()), UUID.randomUUID()));
        when(group.getServers()).thenThrow(new RemoteException());
        thrown.expect(Error.class);
        group.onCreate("", server);
    }

    /**
     * Have a server and RPC invocation fails with leader exception and exception do not embed
     * leader information in it. Expects a remote exception with message "Raft leader is not
     * elected"
     *
     * @throws Exception
     */
    @Test
    public void onRPCWithoutLeader() throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();
        ConsensusRSMPolicy.ClientPolicy client = spy(ConsensusRSMPolicy.ClientPolicy.class);
        Policy.ServerPolicy server = spy(ConsensusRSMPolicy.ServerPolicy.class);
        client.setServer(server);
        doThrow(new LeaderException("leaderException", null)).when(server).onRPC(method, params);
        thrown.expect(RemoteException.class);
        client.onRPC(method, params);
    }

    /**
     * Have a server and RPC invocation fails with leader exception and exception embeds leader
     * information in it. Does RPC invocation to the leader obtained from leader exception
     * information.
     *
     * @throws Exception
     */
    @Test
    public void onRPCWithLeader() throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Let the current rpc server in the client be updated with leader. This happens on the first RPC invocation. So just make an RPC */
        this.client.onRPC(method, params);

        /* Now, get the current rpc server from client. We are pretty sure it raft leader */
        ConsensusRSMPolicy.ServerPolicy leaderServer =
                (ConsensusRSMPolicy.ServerPolicy) this.client.getServer();

        ConsensusRSMPolicy.ClientPolicy localClient = spy(ConsensusRSMPolicy.ClientPolicy.class);
        Policy.ServerPolicy server = spy(ConsensusRSMPolicy.ServerPolicy.class);
        /* Inject the stubbed server to be an rpc sever to client policy object and make the RPC to fail with leader exception containing actual leaderServer's reference in exception */
        localClient.setServer(server);
        doThrow(new LeaderException("leaderException", leaderServer))
                .when(server)
                .onRPC(method, params);
        localClient.onRPC(method, params);
    }

    /**
     * Have 3 servers and RPC invocation fails with remote exception for first 2 servers and
     * succeeds on the last server
     *
     * @throws Exception
     */
    @Test
    public void rpcSucceedOnLastSrv1stAnd2ndSrvRemoteExcept() throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Create a client policy, 3 server policy, group policy objects and inject group and current rpc server to client */
        ConsensusRSMPolicy.ClientPolicy client = spy(ConsensusRSMPolicy.ClientPolicy.class);
        Policy.ServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.ServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.ServerPolicy server3 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.GroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        List<Policy.ServerPolicy> list = new ArrayList<Policy.ServerPolicy>();
        list.add(server1);
        list.add(server2);
        list.add(server3);
        doReturn(list).when(group).getServers();
        client.setServer(server1);
        client.onCreate(group);

        /* Make server1 and server2 to throw remote exceptions and server3 to return successfully */
        doThrow(RemoteException.class).when(server1).onRPC(method, params);
        doThrow(RemoteException.class).when(server2).onRPC(method, params);
        doReturn(new KernelOID(1)).when(server1).$__getKernelOID();
        doReturn(new KernelOID(2)).when(server2).$__getKernelOID();
        doReturn(new KernelOID(3)).when(server3).$__getKernelOID();
        doReturn("OK").when(server3).onRPC(method, params);
        client.onRPC(method, params);
    }

    /**
     * Have 3 servers and RPC invocation fails with remote exception for first server, with leader
     * exception for second server having leader information in exception and finally succeeds on
     * the last attempt
     *
     * @throws Exception
     */
    @Test
    public void rpcSucceedOnLastSrv1stSrvRemoteExcept2ndSrvLeaderExceptWithLeader()
            throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Let the current rpc server in the client be updated with leader. This happens on the first RPC invocation. So just make an RPC */
        this.client.onRPC(method, params);

        /* Now, get the current rpc server from client. We are pretty sure it raft leader */
        ConsensusRSMPolicy.ServerPolicy leaderServer =
                (ConsensusRSMPolicy.ServerPolicy) this.client.getServer();

        /* Create a client policy, 3 server policy, group policy objects and inject group and current rpc server to client */
        ConsensusRSMPolicy.ClientPolicy localClient = spy(ConsensusRSMPolicy.ClientPolicy.class);
        Policy.ServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.ServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.GroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        List<Policy.ServerPolicy> list = new ArrayList<Policy.ServerPolicy>();
        list.add(server1);
        list.add(server2);
        doReturn(list).when(group).getServers();

        /* Inject stubbed server to be an rpc sever to client object */
        localClient.setServer(server1);
        localClient.onCreate(group);

        /* Make server1 to throw remote exception, server2 to throw leader exception with actual leader's reference in exception */
        doThrow(RemoteException.class).when(server1).onRPC(method, params);
        doThrow(new LeaderException("leaderException", leaderServer))
                .when(server2)
                .onRPC(method, params);
        doReturn(new KernelOID(1)).when(server1).$__getKernelOID();
        doReturn(new KernelOID(2)).when(server2).$__getKernelOID();
        localClient.onRPC(method, params);
    }

    /**
     * Have 3 servers and RPC invocation fails with remote exception for first server, with leader
     * exception for second server without leader information in exception(i.e., leader is not
     * elected yet). Expects a remote exception with message "Raft leader is not elected"
     *
     * @throws Exception
     */
    @Test
    public void rpcSucceedOnLastSrv1stSrvRemoteExcept2ndSrvLeaderExceptWithOutLeader()
            throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Create a client policy, 3 server policy, group policy objects and inject group and current rpc server to client */
        ConsensusRSMPolicy.ClientPolicy localClient = spy(ConsensusRSMPolicy.ClientPolicy.class);
        Policy.ServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.ServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.GroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        List<Policy.ServerPolicy> list = new ArrayList<Policy.ServerPolicy>();
        list.add(server1);
        list.add(server2);
        doReturn(list).when(group).getServers();

        /* Inject stubbed server to be an rpc sever to client object */
        localClient.setServer(server1);
        localClient.onCreate(group);

        /* Make server1 to throw remote exception, server2 to throw leader exception with actual leader's reference in exception */
        doThrow(RemoteException.class).when(server1).onRPC(method, params);
        doThrow(new LeaderException("leaderException", null)).when(server2).onRPC(method, params);
        doReturn(new KernelOID(1)).when(server1).$__getKernelOID();
        doReturn(new KernelOID(2)).when(server2).$__getKernelOID();
        thrown.expect(RemoteException.class);
        localClient.onRPC(method, params);
    }

    /**
     * Have 2 servers and both of them throws remote exception. Expects remote exception with
     * message "Failed to connect atleast one server"
     *
     * @throws Exception
     */
    @Test
    public void onRPCServersUnreachable() throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Create a client policy, 2 server policy, group policy objects and inject group and current rpc server to client */

        ConsensusRSMPolicy.ClientPolicy client = spy(ConsensusRSMPolicy.ClientPolicy.class);
        Policy.ServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.ServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        Policy.GroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        List<Policy.ServerPolicy> list = new ArrayList<Policy.ServerPolicy>();
        list.add(server1);
        list.add(server2);
        doReturn(list).when(group).getServers();
        client.setServer(server1);
        client.onCreate(group);

        /* Make both server1 and server2 to throw remote exceptions(i.e., none of them are reachable) */
        doThrow(RemoteException.class).when(server1).onRPC(method, params);
        doThrow(RemoteException.class).when(server2).onRPC(method, params);
        doReturn(new KernelOID(1)).when(server1).$__getKernelOID();
        doReturn(new KernelOID(2)).when(server2).$__getKernelOID();
        thrown.expect(RemoteException.class);
        client.onRPC(method, params);
    }

    /**
     * Have 3 raft servers with leader elected. Bring down the leader and check for re-election of a
     * new leader among the other 2 servers
     *
     * @throws Exception
     */
    @Test
    public void testNodeFailure() throws Exception {
        Server newRaftServer[] = new Server[SERVER_COUNT - 1];

        /* Bring down the raft leader */
        Whitebox.invokeMethod(group, "terminate", server3);

        // Creating a new array with the running raftServers
        newRaftServer[0] = raftServer[0];
        newRaftServer[1] = raftServer[1];

        /* Wait for LEADER_HEARTBEAT_TIMEOUT time. So that other servers can detect current leader as dead and start
        re-election of a new leader. Since Thread.sleep time is subjected to the precision and accuracy of system timers
        and schedulers, added another 100ms to it. */
        Thread.sleep(LEADER_HEARTBEAT_TIMEOUT + 100);
        // Verifying that new leader has been elected from amongst the running raftServers,
        // upon failure of the initial leader.
        verifyLeaderElected(newRaftServer);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
