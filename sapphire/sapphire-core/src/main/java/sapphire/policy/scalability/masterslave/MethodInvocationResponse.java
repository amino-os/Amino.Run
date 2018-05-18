package sapphire.policy.scalability.masterslave;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author terryz
 */
public class MethodInvocationResponse implements Serializable {
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

    public MethodInvocationResponse(ReturnCode returnCode, Object result) {
        this.returnCode = returnCode;
        this.result = result;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public Object getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInvocationResponse)) return false;
        MethodInvocationResponse that = (MethodInvocationResponse) o;
        return getReturnCode() == that.getReturnCode() &&
                Objects.equals(getResult(), that.getResult());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReturnCode(), getResult());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MethodInvocationResponse{");
        sb.append("returnCode=").append(returnCode);
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}
