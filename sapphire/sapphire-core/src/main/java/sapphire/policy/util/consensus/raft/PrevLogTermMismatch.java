package sapphire.policy.util.consensus.raft;

/** Created by quinton on 5/25/18. */

/**
 * If log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm on appendEntries
 * RPC
 */
public class PrevLogTermMismatch extends Exception {
    int logIndex, remoteTerm, localTerm;

    public PrevLogTermMismatch(String s, int logIndex, int remoteTerm, int localTerm) {
        super(s);
        this.logIndex = logIndex;
        this.remoteTerm = remoteTerm;
        this.localTerm = localTerm;
    }
}
