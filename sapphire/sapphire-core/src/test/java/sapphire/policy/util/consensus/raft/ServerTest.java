package sapphire.policy.util.consensus.raft;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.replication.ConsensusRSMPolicy;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by quinton on 3/16/18.
 */
public class ServerTest {
    final int SERVER_COUNT = 3;
    SapphirePolicy.SapphireServerPolicy[] serverPolicy = new SapphirePolicy.SapphireServerPolicy[SERVER_COUNT];
    Server raftServer[] = new Server[SERVER_COUNT];
    private AppObject appObject;

    @Before
    public void setUp() throws Exception {
        appObject = mock(AppObject.class);
        for (int i=0; i<SERVER_COUNT; i++) {
            serverPolicy[i] = spy(ConsensusRSMPolicy.ServerPolicy.class);
            serverPolicy[i].$__initialize(appObject);
            raftServer[i] = new Server((ConsensusRSMPolicy.ServerPolicy)serverPolicy[i]);
        }
        for (int i=0; i<SERVER_COUNT; i++) {
            for(int j=0;j<SERVER_COUNT; j++) {
                if (i!= j) {
                    raftServer[i].addServer(raftServer[j].getMyServerID(), raftServer[j]);
                }
            }
        }
        /*
        so = new LockingTransactionTestStub();
        appObject = new AppObject(so);
        this.server = spy(LockingTransactionPolicy.ServerPolicy.class);
        this.server.$__initialize(appObject);
        */
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void getMyServerID() throws Exception {
        raftServer[0].getMyServerID();
    }

    @Test
    public void getCurrentLeader() throws Exception {
        raftServer[0].getCurrentLeader();
    }

    @Test
    public void majorityQuorumSize() throws Exception {
        raftServer[0].majorityQuorumSize();
    }

    @Test
    public void fullQuorumSize() throws Exception {
        raftServer[0].fullQuorumSize();
    }

    @Test
    public void getServer() throws Exception {
        assert(raftServer[0].getServer(raftServer[1].getMyServerID()) == raftServer[1]);
    }

    @Test
    public void addServer() throws Exception {
        // Not necessary, tested in setUp() above.
    }

    @Test
    public void start() throws Exception {
        raftServer[0].start();
    }

    @Test
    public void become() throws Exception {
        raftServer[0].start();
        raftServer[0].become(Server.State.FOLLOWER, Server.State.FOLLOWER);
        assert(raftServer[0].getState() == Server.State.FOLLOWER);
        raftServer[0].become(Server.State.CANDIDATE, Server.State.FOLLOWER);
        assert(raftServer[0].getState() == Server.State.CANDIDATE);
        raftServer[0].become(Server.State.LEADER, Server.State.CANDIDATE);
        assert(raftServer[0].getState() == Server.State.LEADER);
    }

    @Test
    public void successfulLeaderElection() throws java.lang.InterruptedException {
        for(Server s: raftServer) {
            s.start();
        }
        raftServer[0].become(Server.State.CANDIDATE, Server.State.FOLLOWER); // Immediately tell the first one to start an election, just to speed things up in this unit test.

        int leaderCount = 0;
        UUID initialLeader = null, finalLeader=null;
        long startTime = System.currentTimeMillis();
        while (leaderCount==0 && System.currentTimeMillis() < startTime + Server.LEADER_HEARTBEAT_TIMEOUT) {
            // Use LEADER_HEARTBEAT_TIMEOUT not LEADER_ELECTION_TIMEOUT as the latter is randomized, and we want consistent test results.
            // LEADER_HEARTBEAT_TIMEOUT is currently defined to always be > LEADER_ELECTION_TIMEOUT
            for (Server s : raftServer) {
                if (s.getState() == Server.State.LEADER) {
                    initialLeader = s.getMyServerID();
                    leaderCount++;
                }
            }
        }
        assertEquals(1, leaderCount); // So we got one leader, at least.
        Thread.sleep(100); // Wait a while and make sure that we still have exactly one leader, and that it's the same.
        leaderCount=0;
        for (Server s: raftServer) {
            if (s.getState() == Server.State.LEADER) {
                finalLeader = s.getMyServerID();
                leaderCount++;
            }
        }
        assertTrue(leaderCount==1 && finalLeader.equals(initialLeader));
    }

    @Test
    public void appendEntries() throws Exception {
    }

    @Test
    public void requestVote() throws Exception {
        // Already tested by becoming a candidate above.
    }

    @Test
    public void applyCommitted() throws Exception {
    }

    @Test
    public void respondToRemoteTerm() throws Exception {
    }

    @Test
    public void doRPC() throws java.lang.Exception {
        successfulLeaderElection();
        String[] methods = { "fooMethod", "barMethod" };
        ArrayList<Object> args = new ArrayList<Object>();
        for (String method : methods) {
            // Apply to statemachine is invoked only on leader
            raftServer[0].applyToStateMachine(new ConsensusRSMPolicy.RPC(method, args));
        }
    }
}