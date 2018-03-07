package sapphire.runtime;

import java.io.Serializable;
import java.util.ArrayList;

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

    private final String methodName;
    private final ArrayList<Object> params;
    private final MethodType methodType;

    private MethodInvocationRequest(Builder builder) {
        this.methodName = builder.methodName;
        this.methodType = builder.methodType;
        this.params = builder.params;
    }

    public final static Builder newBuilder() {
        return new Builder();
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInvocationRequest)) return false;

        MethodInvocationRequest that = (MethodInvocationRequest) o;

        if (!getMethodName().equals(that.getMethodName())) return false;

        if (getParams() == null ) {
            return that.getParams() != null;
        } else {
            if (!getParams().equals(that.getParams())) return false;
        }
        return getMethodType() == that.getMethodType();
    }

    @Override
    public int hashCode() {
        int result = getMethodName().hashCode();
        result = 31 * result + getParams().hashCode();
        result = 31 * result + getMethodType().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MethodInvocationRequest{" +
                "methodName='" + methodName + '\'' +
                ", params=" + params +
                ", methodType=" + methodType +
                '}';
    }

    public static class Builder {
        private String methodName;
        private ArrayList<Object> params;
        private MethodType methodType;

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
            if (null == methodName || methodName.trim().isEmpty()) {
                throw new IllegalArgumentException("method name is not specified");
            }
            return new MethodInvocationRequest(this);
        }
    }
}
