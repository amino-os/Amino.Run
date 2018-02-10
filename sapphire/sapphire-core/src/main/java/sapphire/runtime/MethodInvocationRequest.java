package sapphire.runtime;

import java.io.Serializable;
import java.util.ArrayList;

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

    public ArrayList<Object> getParams() {
        return params;
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodType getMethodType() {
        return methodType;
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

        public Builder(String methodName) {
            this.methodName = methodName;
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
