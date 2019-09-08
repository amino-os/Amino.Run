package amino.run.policy.scalability.masterslave;

import java.io.Serializable;

/** @author terryz */
public class MethodInvocationResponse implements Serializable {
    /** Valid Return Codes */
    public enum ReturnCode {
        /**
         * Method invocation succeeded in which case {@link MethodInvocationResponse#result} stores
         * the result
         */
        SUCCESS,

        /**
         * Method invocation failed in which case {@link MethodInvocationResponse#result} stores the
         * {@link Throwable} that caused the failure
         */
        FAILURE,

        /** Indicating that the request should be redirected to other servers */
        REDIRECT
    }

    /** Method invocation return code */
    private final ReturnCode returnCode;

    /** Method invocation result */
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MethodInvocationResponse that = (MethodInvocationResponse) o;

        if (returnCode != that.returnCode) {
            return false;
        }
        return result != null ? result.equals(that.result) : that.result == null;
    }

    @Override
    public int hashCode() {
        int result1 = returnCode != null ? returnCode.hashCode() : 0;
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        return result1;
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
