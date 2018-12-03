package sapphire.policy.util.consensus.raft;

import java.util.List;

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
}
