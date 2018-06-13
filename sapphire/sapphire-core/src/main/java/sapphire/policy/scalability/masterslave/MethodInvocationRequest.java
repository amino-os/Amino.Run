package sapphire.policy.scalability.masterslave;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import static sapphire.policy.scalability.masterslave.MethodInvocationRequest.MethodType.IMMUTABLE;

/**
 * Method invocation request on App Objects.
 *
 * @author terryz
 */
public final class MethodInvocationRequest implements Serializable {
    /**
     * Method types
     */
    public enum MethodType {
        IMMUTABLE, MUTABLE
    };

    private final String clientId;
    private final Long requestId;
    private final String methodName;
    private final ArrayList<Object> params;
    private final MethodType methodType;

    public MethodInvocationRequest(String clientId,
                                    Long requestId,
                                    String methodName,
                                    ArrayList<Object> params,
                                    MethodType methodType) {
        this.clientId = clientId;
        this.requestId = requestId;
        this.methodName = methodName;
        this.methodType = methodType;
        this.params = params;
    }

    public final String getClientId() { return clientId; }

    public final long getRequestId() { return requestId; }

    public final ArrayList<Object> getParams() {
        return params;
    }

    public final String getMethodName() {
        return methodName;
    }

    public final MethodType getMethodType() {
        return methodType;
    }

    public final boolean isImmutable() {
        return getMethodType() != null && getMethodType() == IMMUTABLE;
    }

    public final boolean isMutable() {
        return !isImmutable();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MethodInvocationRequest{");
        sb.append("clientId='").append(clientId).append('\'');
        sb.append(", requestId=").append(requestId);
        sb.append(", methodName='").append(methodName).append('\'');
        sb.append(", params=").append(params);
        sb.append(", methodType=").append(methodType);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInvocationRequest)) return false;
        MethodInvocationRequest that = (MethodInvocationRequest) o;
        return Objects.equals(getClientId(), that.getClientId()) &&
                Objects.equals(getRequestId(), that.getRequestId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClientId(), getRequestId());
    }
}
