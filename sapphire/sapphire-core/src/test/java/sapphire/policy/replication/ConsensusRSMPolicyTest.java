package sapphire.policy.replication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.policy.util.consensus.raft.Server.LEADER_HEARTBEAT_TIMEOUT;
import static sapphire.policy.util.consensus.raft.ServerTest.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.*;
import sapphire.kernel.common.KernelOID;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.consensus.raft.LeaderException;
import sapphire.policy.util.consensus.raft.LogEntry;
import sapphire.policy.util.consensus.raft.Server;
import sapphire.sampleSO.SO;

/** Created by terryz on 4/9/18. */
@RunWith(PowerMockRunner.class)
public class ConsensusRSMPolicyTest extends BaseTest {
    final int SERVER_COUNT = 3;
    Server raftServer[] = new Server[SERVER_COUNT];
    SO so1, so2, so3;

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        this.serversInSameRegion = false;

        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.sampleSO.SO")
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(ConsensusRSMPolicy.class.getName())
                                        .create())
                        .create();
        super.setUp(
                3,
                spec,
                new HashMap<String, Class>() {
                    {
                        put("ConsensusRSMPolicy", ConsensusRSMPolicy.GroupPolicy.class);
                    }
                },
                new HashMap<String, Class>() {
                    {
                        put("ConsensusRSMPolicy", ConsensusRSMPolicy.ServerPolicy.class);
                    }
                });

        /* Make server3 as raft leader */
        raftServer[0] =
                (sapphire.policy.util.consensus.raft.Server)
                        extractFieldValueOnInstance(server1, "raftServer");
        raftServer[1] =
                (sapphire.policy.util.consensus.raft.Server)
                        extractFieldValueOnInstance(server2, "raftServer");
        raftServer[2] =
                (sapphire.policy.util.consensus.raft.Server)
                        extractFieldValueOnInstance(server3, "raftServer");
        makeFollower(raftServer[0]);
        makeFollower(raftServer[1]);
        makeCandidate(raftServer[2]);

        assertTrue(raftServer[verifyLeaderElected(raftServer)] == raftServer[2]);

        so1 = ((SO) (server1.sapphire_getAppObject().getObject()));
        so2 = ((SO) (server2.sapphire_getAppObject().getObject()));
        so3 = ((SO) (server3.sapphire_getAppObject().getObject()));
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
        String method = "public void sapphire.sampleSO.SO.incI()";
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
        SapphirePolicy.SapphireServerPolicy server = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireGroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        doReturn(true).when(server).isAlreadyPinned();
        when(group.getServers()).thenThrow(new RemoteException());
        thrown.expect(Error.class);
        group.onCreate("", server, new SapphireObjectSpec());
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
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();
        SapphirePolicy.SapphireClientPolicy client = spy(ConsensusRSMPolicy.ClientPolicy.class);
        SapphirePolicy.SapphireServerPolicy server = spy(ConsensusRSMPolicy.ServerPolicy.class);
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
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Let the current rpc server in the client be updated with leader. This happens on the first RPC invocation. So just make an RPC */
        this.client.onRPC(method, params);

        /* Now, get the current rpc server from client. We are pretty sure it raft leader */
        ConsensusRSMPolicy.ServerPolicy leaderServer =
                (ConsensusRSMPolicy.ServerPolicy) this.client.getServer();

        SapphirePolicy.SapphireClientPolicy localClient =
                spy(ConsensusRSMPolicy.ClientPolicy.class);
        SapphirePolicy.SapphireServerPolicy server = spy(ConsensusRSMPolicy.ServerPolicy.class);

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
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Create a client policy, 3 server policy, group policy objects and inject group and current rpc server to client */
        SapphirePolicy.SapphireClientPolicy client = spy(ConsensusRSMPolicy.ClientPolicy.class);
        SapphirePolicy.SapphireServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireServerPolicy server3 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireGroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        group.addServer(server1);
        group.addServer(server2);
        group.addServer(server3);
        client.setServer(server1);
        client.onCreate(group, null);

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
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Let the current rpc server in the client be updated with leader. This happens on the first RPC invocation. So just make an RPC */
        this.client.onRPC(method, params);

        /* Now, get the current rpc server from client. We are pretty sure it raft leader */
        ConsensusRSMPolicy.ServerPolicy leaderServer =
                (ConsensusRSMPolicy.ServerPolicy) this.client.getServer();

        /* Create a client policy, 3 server policy, group policy objects and inject group and current rpc server to client */
        SapphirePolicy.SapphireClientPolicy localClient =
                spy(ConsensusRSMPolicy.ClientPolicy.class);
        SapphirePolicy.SapphireServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireGroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        group.addServer(server1);
        group.addServer(server2);

        /* Inject stubbed server to be an rpc sever to client object */
        localClient.setServer(server1);
        localClient.onCreate(group, null);

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
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Create a client policy, 3 server policy, group policy objects and inject group and current rpc server to client */
        SapphirePolicy.SapphireClientPolicy localClient =
                spy(ConsensusRSMPolicy.ClientPolicy.class);
        SapphirePolicy.SapphireServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireGroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        group.addServer(server1);
        group.addServer(server2);

        /* Inject stubbed server to be an rpc sever to client object */
        localClient.setServer(server1);
        localClient.onCreate(group, null);

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
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        /* Create a client policy, 2 server policy, group policy objects and inject group and current rpc server to client */
        SapphirePolicy.SapphireClientPolicy client = spy(ConsensusRSMPolicy.ClientPolicy.class);
        SapphirePolicy.SapphireServerPolicy server1 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireServerPolicy server2 = spy(ConsensusRSMPolicy.ServerPolicy.class);
        SapphirePolicy.SapphireGroupPolicy group = spy(ConsensusRSMPolicy.GroupPolicy.class);
        group.addServer(server1);
        group.addServer(server2);
        client.setServer(server1);
        client.onCreate(group, null);

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
        this.server3.sapphire_remove_replica();

        // Creating a new array with the running raftServers
        newRaftServer[0] = raftServer[0];
        newRaftServer[1] = raftServer[1];

        // Waiting for LEADER_HEARTBEAT_TIMEOUT before re-election starts
        Thread.sleep(LEADER_HEARTBEAT_TIMEOUT);
        // Verifying that new leader has been elected from amongst the running raftServers,
        // upon failure of the initial leader.
        verifyLeaderElected(newRaftServer);
    }

    @After
    public void tearDown() throws Exception {
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
