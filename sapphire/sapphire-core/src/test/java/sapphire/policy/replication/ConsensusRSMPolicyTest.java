package sapphire.policy.replication;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.policy.util.consensus.raft.ServerTest.getCurrentLeader;
import static sapphire.policy.util.consensus.raft.ServerTest.makeFollower;
import static sapphire.policy.util.consensus.raft.ServerTest.makeLeader;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.Language;
import sapphire.app.SO;
import sapphire.app.SapphireObject;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.stubs.SO_Stub;
import sapphire.common.BaseTest;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.common.Utils;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.consensus.raft.LeaderException;
import sapphire.policy.util.consensus.raft.RemoteRaftServer;
import sapphire.runtime.Sapphire;

/** Created by terryz on 4/9/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class ConsensusRSMPolicyTest extends BaseTest {
    sapphire.policy.util.consensus.raft.Server raftServer1;
    sapphire.policy.util.consensus.raft.Server raftServer2;
    sapphire.policy.util.consensus.raft.Server raftServer3;

    public static class ConsensusSO extends SO implements SapphireObject<ConsensusRSMPolicy> {}

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
        this.serversInSameRegion = false;
        super.setUp(Server_Stub.class, Group_Stub.class);
        SapphireObjectSpec spec = new SapphireObjectSpec();
        spec.setLang(Language.java);
        spec.setJavaClassName("sapphire.policy.replication.ConsensusRSMPolicyTest$ConsensusSO");

        SapphireObjectID sapphireObjId = spiedOms.createSapphireObject(spec.toString());
        soStub = (SO_Stub) spiedOms.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
        so = ((SO) (server1.sapphire_getAppObject().getObject()));

        /* Ensure the order of servers is same as in server list in group */
        ArrayList<SapphirePolicy.SapphireServerPolicy> servers =
                this.client.getGroup().getServers();
        this.server1 = (ConsensusRSMPolicy.ServerPolicy) servers.get(0);
        this.server2 = (ConsensusRSMPolicy.ServerPolicy) servers.get(1);
        this.server3 = (ConsensusRSMPolicy.ServerPolicy) servers.get(2);
        this.client.setServer(this.server1);

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

        RemoteRaftServer leaderViewOfRaftServer1 = getCurrentLeader(raftServer1);
        while (leaderViewOfRaftServer1 == null) {
            sleep(100);
            leaderViewOfRaftServer1 = getCurrentLeader(raftServer1);
        }

        RemoteRaftServer leaderViewOfRaftServer2 = getCurrentLeader(raftServer2);
        while (leaderViewOfRaftServer2 == null) {
            sleep(100);
            leaderViewOfRaftServer2 = getCurrentLeader(raftServer2);
        }

        assert (leaderViewOfRaftServer1 == this.server3);
        assert (leaderViewOfRaftServer2 == this.server3);
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
        this.group.onCreate(this.server1, new HashMap<>());
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
        sleep(1000);
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
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
