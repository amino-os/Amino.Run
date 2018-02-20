package sapphire.runtime;

/**
 * @author terryz
 */
public class MethodInvocationResponse {
    /**
     * Valid Return Codes
     */
    public enum ReturnCode {
        /**
         * Method invocation succeeded in which case
         * {@link MethodInvocationResponse#result} stores
         * the result
         */
        SUCCESS,

        /**
         * Method invocation failed in which case
         * {@link MethodInvocationResponse#result} stores the
         * {@link Throwable} that caused the failure
         */
        FAILURE,

        /**
         * Indicating that the request should be redirected to
         * other servers
         */
        REDIRECT
    }

    /**
     * Method invocation return code
     */
    private final ReturnCode returnCode;

    /**
     * Method invocation result
     */
    private Object result;

    private MethodInvocationResponse(Builder builder) {
        this.returnCode = builder.returnCode;
        this.result = builder.result;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public Object getResult() {
        return result;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "MethodInvocationResponse{" +
                "returnCode=" + returnCode +
                ", result=" + result +
                '}';
    }

    //
    // MethodInvocationResponse Builder
    //
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

        public MethodInvocationResponse build() {
            if (returnCode == null) {
                throw new NullPointerException("returnCode is null");
            }

            return new MethodInvocationResponse(this);
        }
    }
}
