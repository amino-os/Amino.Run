package sapphire.runtime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import static sapphire.runtime.MethodInvocationRequest.MethodType.READ;
import static sapphire.runtime.MethodInvocationRequest.MethodType.WRITE;

/**
 * @author terryz
 */
public final class MethodInvocationRequest implements Serializable {
    /**
     * Method types
     */
    public enum MethodType {READ, WRITE};

    private final String clientId;
    private final Long requestId;
    private final String methodName;
    private final ArrayList<Object> params;
    private final MethodType methodType;

    private MethodInvocationRequest(Builder builder) {
        this.clientId = builder.clientId;
        this.requestId = builder.requestId;
        this.methodName = builder.methodName;
        this.methodType = builder.methodType;
        this.params = builder.params;
    }

    public final static Builder newBuilder() {
        return new Builder();
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

    public final boolean isRead() {
        return getMethodType() != null && getMethodType() == READ;
    }

    public final boolean isWrite() {
        return getMethodType() != null && getMethodType() == WRITE;
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
                Objects.equals(getRequestId(), that.getRequestId()) &&
                Objects.equals(getMethodName(), that.getMethodName()) &&
                Objects.equals(getParams(), that.getParams()) &&
                getMethodType() == that.getMethodType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClientId(), getRequestId(), getMethodName(), getParams(), getMethodType());
    }

    public static class Builder {
        private String clientId;
        private long requestId;
        private String methodName;
        private ArrayList<Object> params;
        private MethodType methodType;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder requestId(long requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder params(ArrayList<Object> params) {
            this.params = params;
            return this;
        }

        public Builder methodType(MethodType type) {
            this.methodType = type;
            return this;
        }

        public MethodInvocationRequest build() {
            if (clientId == null) {
                throw new NullPointerException("client ID not specified");
            }

            if (methodName == null || methodName.trim().isEmpty()) {
                throw new IllegalArgumentException("method name is not specified");
            }
            return new MethodInvocationRequest(this);
        }
    }
}
