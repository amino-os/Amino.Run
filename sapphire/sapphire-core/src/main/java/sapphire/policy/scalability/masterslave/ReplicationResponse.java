package sapphire.policy.scalability.masterslave;

import java.io.Serializable;

/**
 * Response for entry replications from slave to master
 *
 * @author terryz
 */
public class ReplicationResponse implements Serializable {

    /**
     * Valid Return Codes
     */
    public enum ReturnCode {
        /**
         * Replication succeeded. It may be a partial success.
         * The result is a <code>Long</code> that refers to
         * the largest successfully replicated log entry index
         * on the slave.
         */
        SUCCESS,

        /**
         * Replication failed invocation failed in which case
         * {@link #result} stores the
         * {@link Throwable} that caused the failure
         */
        FAILURE,

        /**
         *
         */
        TRACEBACK
    }

    /**
     * Method invocation return code
     */
    private final ReturnCode returnCode;

    /**
     * Method invocation result
     */
    private final Object result;

    public ReplicationResponse(ReturnCode returnCode, Object result) {
        this.returnCode = returnCode;
        this.result = result;
    }

    /**
     * @return return code
     */
    public ReturnCode getReturnCode() {
        return returnCode;
    }

    /**
     * @return result
     */
    public Object getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "ReplicationResponse{" +
                "returnCode=" + returnCode +
                ", result=" + result +
                '}';
    }
}
