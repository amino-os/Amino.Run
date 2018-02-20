package sapphire.policy.scalability;

/**
 * @author terryz
 */
public class ReplicationResponse {

    /**
     * Valid Return Codes
     */
    public enum ReturnCode {
        /**
         * Replication succeeded
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

    public static Builder newBuilder() {
        return new Builder();
    }

    private ReplicationResponse(Builder builder) {
        this.returnCode = builder.returnCode;
        this.result = builder.result;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

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

    public static class Builder {
        private ReturnCode returnCode;
        private Object result;

        public Builder returnCode(ReturnCode returnCode) {
            this.returnCode = returnCode;
            return this;
        }

        public Builder result(Object result) {
            this.result = result;
            return this;
        }

        public ReplicationResponse build() {
            if (returnCode == null) {
                throw new NullPointerException("returnCode is null");
            }

            return new ReplicationResponse(this);
        }
    }
}
