package sapphire.policy.util.consensus.raft;

import static java.lang.Thread.sleep;

import java.util.List;

/**
 * This Util file has been created for the purpose of unit tests only. The reason for creating this
 * utility, is to use the methods provided in "sapphire.policy.util.consensus.raft.Server" which has
 * default access.
 */

/** Created by Vishwajeet on 06/12/18 */
public class RaftUtil {
    public static PersistentState getRaftpstate(Server r) throws Exception {
        return r.pState;
    }

    public static VolatileState getRaftvstate(Server r) throws Exception {
        return r.vState;
    }

    public static List getRaftlog(Server r) throws Exception {
        return r.pState.log();
    }

    public static int getRaftLastApplied(Server r) throws Exception {
        return r.vState.getLastApplied();
    }

    public static int getraftCommitIndex(Server r) throws Exception {
        return r.vState.getCommitIndex();
    }

    public static Object getOperation(LogEntry l) throws Exception {
        return l.operation;
    }

    public static Integer getTerm(LogEntry l) throws Exception {
        return l.term;
    }

    public static void verifyLeaderElected(int maxRetries, Server[] s)
            throws Exception, InterruptedException {
        int count = 0;
        int leaderCount = 0;
        while (count < maxRetries) {
            for (Server s1 : s) {
                if (s1.getState() == Server.State.LEADER) {
                    leaderCount++;
                }
            }
            count++;
            if (leaderCount == 1) {
                break;
            } else {
                sleep(100);
            }
        }
    }

    public static void verifyCommitIndex(int maxRetries, Server r1, Server r2)
            throws Exception, InterruptedException {
        int count = 0;
        while (count < maxRetries) {
            if (r1.vState.getCommitIndex() != r2.vState.getCommitIndex()) {
                count++;
                sleep(500);
            } else {
                break;
            }
        }
    }
}
