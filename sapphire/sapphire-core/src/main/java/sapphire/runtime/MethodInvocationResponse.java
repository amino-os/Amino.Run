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
        FAIL,

        /**
         * Indicating that the request should be redirected to
         * other servers in which case {@link MethodInvocationResponse#result}
         * stores a list of valid servers
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

    /**
     * Private Constructor
     * @param builder
     */
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

        public Builder(ReturnCode returnCode, Object result) {
            if (returnCode == null) {
                throw new NullPointerException("returnCode is null");
            }

            this.returnCode = returnCode;
            this.result = result;
        }

        public MethodInvocationResponse build() {
            return new MethodInvocationResponse(this);
        }
    }
}
