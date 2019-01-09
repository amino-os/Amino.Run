package amino.run.policy.util.consensus.raft;

/** Created by quinton on 5/25/18. */

/**
 * Base class for all voting exceptions. When candidates request a vote but are denied, they need to
 * know the current term of the voter to update themselves.
 */
public class VotingException extends java.lang.Exception {
    public int currentTerm;

    public VotingException(String s, int currentTerm) {
        super(s);
        this.currentTerm = currentTerm;
    }
}
