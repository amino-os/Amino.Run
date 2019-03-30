package amino.run.policy.util.consensus.raft;

/** Created by quinton on 5/25/18. */

/** If member has already voted for a different leader when receiving requestVote RPC */
public class AlreadyVotedException extends VotingException {
    public AlreadyVotedException(String s, int currentTerm) {
        super(s, currentTerm);
    }
}
