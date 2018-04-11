package sapphire.policy.transaction;

import java.util.Objects;

/**
 * utility class for 2PC primitive verbs
 */
class TwoPCPrimitive {
    final static String VoteReq = "tx_vote_req";
    final static String Commit = "tx_commit";
    final static String Abort = "tx_abort";

    /**
     * checks for the vote_req promitive verb
     * @param methodName the input to be checked
     * @return the check result
     */
    static boolean isVoteRequest(String methodName) {
        return Objects.equals(methodName, VoteReq);
    }

    /**
     * checks for transaction commit verb
     * @param methodName the input to be checked
     * @return the check result
     */
    static boolean isCommit(String methodName) {
        return Objects.equals(methodName, Commit);
    }

    /**
     * checks for transaction abort verb
     * @param methodName the input to be checked
     * @return the check result
     */
    static boolean isAbort(String methodName) {
        return Objects.equals(methodName, Abort);
    }
}
