package sapphire.policy.util.consensus.raft;

/** Created by quinton on 5/25/18. */

/** If term < currentTerm on appendEntries or requestVote RPC */
public class InvalidTermException extends VotingException {
    public InvalidTermException(String s, int currentTerm) {
        super(s, currentTerm);
    }
}
