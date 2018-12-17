package sapphire.policy.replication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.policy.util.consensus.raft.Server.LEADER_HEARTBEAT_TIMEOUT;
import static sapphire.policy.util.consensus.raft.ServerTest.*;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.*;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.consensus.raft.LeaderException;
import sapphire.policy.util.consensus.raft.LogEntry;
import sapphire.policy.util.consensus.raft.Server;
import sapphire.sampleSO.SO;
import sapphire.sampleSO.stubs.SO_Stub;

/** Created by terryz on 4/9/18. */
@RunWith(PowerMockRunner.class)
public class ConsensusRSMPolicyTest extends BaseTest {
    final int SERVER_COUNT = 3;
    Server raftServer[] = new Server[SERVER_COUNT];
    SO so2, so3;

    public static class ConsensusSO extends SO {}

    public static class Group_Stub extends ConsensusRSMPolicy.GroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

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

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class Server_Stub extends ConsensusRSMPolicy.ServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

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

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

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
                spec,
                new HashMap<String, Class>() {
                    {
                        put("ConsensusRSMPolicy", Group_Stub.class);
                    }
                },
                new HashMap<String, Class>() {
                    {
                        put("ConsensusRSMPolicy", Server_Stub.class);
                    }
                });

        SapphireObjectID sapphireObjId = sapphireObjServer.createSapphireObject(spec.toString());
        soStub1 = (SO_Stub) sapphireObjServer.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub1, "$__client");
        so1 = ((SO) (server1.sapphire_getAppObject().getObject()));
        so2 = ((SO) (server2.sapphire_getAppObject().getObject()));
        so3 = ((SO) (server3.sapphire_getAppObject().getObject()));

        /* Ensure the order of servers is same as in server list in group */
        ArrayList<SapphirePolicy.SapphireServerPolicy> servers =
                this.client.getGroup().getServers();
        this.server1 = (ConsensusRSMPolicy.ServerPolicy) servers.get(0);
        this.server2 = (ConsensusRSMPolicy.ServerPolicy) servers.get(1);
        this.server3 = (ConsensusRSMPolicy.ServerPolicy) servers.get(2);
        this.client.setServer(this.server1);

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
        makeLeader(raftServer[2]);
        assertTrue(raftServer[verifyLeaderElected(raftServer)].equals(raftServer[2]));
    }

    @Test
    public void testSerialization() throws Exception {
        ConsensusRSMPolicy.ClientPolicy client = new ConsensusRSMPolicy.ClientPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(client));

        ConsensusRSMPolicy.GroupPolicy group = new ConsensusRSMPolicy.GroupPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(group));

        ConsensusRSMPolicy.ServerPolicy server = new ConsensusRSMPolicy.ServerPolicy();
        assertNotNull(Utils.ObjectCloner.deepCopy(server));
    }

    @Test
    public void applyToStateMachine() throws Exception {
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();
        ConsensusRSMPolicy.RPC rpc = new ConsensusRSMPolicy.RPC(method, params);
        ConsensusRSMPolicy.ServerPolicy server3 = (ConsensusRSMPolicy.ServerPolicy) this.server3;

        server3.applyToStateMachine(rpc);
        verify(server3, times(1)).apply(Matchers.anyObject());
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

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void omsNotAvailable() throws Exception {
        when(this.group.getServers()).thenThrow(new RemoteException());
        thrown.expect(Error.class);
        this.group.onCreate("", this.server1, new SapphireObjectSpec());
    }

    /**
     * Try onRPC on the first server, if this is not the leader throw LeaderException. Retry rpc on
     * the next server, if leader not elected throw RemoteException.
     */
    @Test
    public void onRPCWithoutLeader() throws Exception {
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(new LeaderException("leaderException", null))
                .when(this.server1)
                .onRPC(method, params);
        thrown.expect(RemoteException.class);
        this.client.onRPC(method, params);
    }

    /**
     * Try onRPC on the non leader server, if this is not the leader throw LeaderException. Retry
     * rpc on the leader server.
     */
    @Test
    public void onRPCWithLeader() throws Exception {
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();
        client.onRPC(method, params);
    }

    /**
     * If onRPC fails on the server with remote exception, get servers from group and find a
     * responding server. Try with next server until you find a responding server.
     */
    @Test
    public void clientOnRPC() throws Exception {
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(RemoteException.class).when(this.server2).onRPC(method, params);
        client.onRPC(method, params);
    }

    /** If onRPC fails to happen on any of the servers, throw RemoteException. */
    @Test
    public void onRPCServersUnreachable() throws Exception {
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();

        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(RemoteException.class).when(this.server2).onRPC(method, params);
        doThrow(RemoteException.class).when(this.server3).onRPC(method, params);
        thrown.expect(RemoteException.class);
        this.client.onRPC(method, params);
    }

    /**
     * If onRPC fails on the leader, get servers from group and find a responding server. If leader
     * still not elected, throw RemoteException.
     */
    @Test
    public void onRPCToFollowerWithoutLeader() throws Exception {
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();
        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(new LeaderException("LeaderException", null))
                .when(this.server2)
                .onRPC(method, params);
        thrown.expect(RemoteException.class);
        this.client.onRPC(method, params);
    }

    /**
     * If onRPC fails on the leader, get servers from group and find a responding server. Store this
     * server as reachable and use it for future rpc.
     */
    @Test
    public void onRPCToFollowerWithLeader() throws Exception {
        String method = "public void sapphire.sampleSO.SO.incI()";
        ArrayList<Object> params = new ArrayList<Object>();
        doThrow(RemoteException.class).when(this.server1).onRPC(method, params);
        doThrow(
                        new LeaderException(
                                "LeaderException", (ConsensusRSMPolicy.ServerPolicy) this.server3))
                .when(this.server2)
                .onRPC(method, params);
        this.client.onRPC(method, params);
    }

    /**
     * Leader in the consensus setup is failed and election of a new leader from amongst the running
     * raft servers is verified.
     */
    @Test
    public void testNodeFailure() throws Exception {
        Server newRaftServer[] = new Server[SERVER_COUNT - 1];
        // Verifying leader already exists
        int indx = verifyLeaderElected(raftServer);
        Server initialLeader = raftServer[indx];

        // Verifying the leader raftServer is same as that made in Setup
        assertTrue(initialLeader.equals(raftServer[2]));

        // Failing the current leader raftServer by calling stop
        initialLeader.stop();

        // Creating a new array with the running raftServers
        System.arraycopy(raftServer, 0, newRaftServer, 0, 2);

        // Waiting for LEADER_HEARTBEAT_TIMEOUT before re- election starts
        Thread.sleep(LEADER_HEARTBEAT_TIMEOUT);
        // Verifying that new leader has been elected from amongst the running raftServers
        // upon failure of the initial leader.
        int indx1 = verifyLeaderElected(newRaftServer);
        Server newLeader = raftServer[indx1];
        assertNotNull(newLeader);
        // Verifying the new leader is not same as the initial one
        assertNotEquals(initialLeader, newLeader);
    }

    @After
    public void tearDown() throws Exception {
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
