package amino.run.policy.transaction;

import java.util.Objects;

/** utility class for 2PC primitive verbs */
class TwoPCPrimitive {
    static final String VoteReq = "tx_vote_req";
    static final String Commit = "tx_commit";
    static final String Abort = "tx_abort";

    /**
     * checks for the vote_req promitive verb
     *
     * @param methodName the input to be checked
     * @return the check result
     */
    static boolean isVoteRequest(String methodName) {
        return Objects.equals(methodName, VoteReq);
    }

    /**
     * checks for transaction commit verb
     *
     * @param methodName the input to be checked
     * @return the check result
     */
    static boolean isCommit(String methodName) {
        return Objects.equals(methodName, Commit);
    }

    /**
     * checks for transaction abort verb
     *
     * @param methodName the input to be checked
     * @return the check result
     */
    static boolean isAbort(String methodName) {
        return Objects.equals(methodName, Abort);
    }
}
