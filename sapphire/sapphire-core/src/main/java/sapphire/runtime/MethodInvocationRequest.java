package sapphire.runtime;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author terryz
 */
// TODO (Terry): Replace with a custom serializer
public class MethodInvocationRequest implements Serializable {
    public enum MethodType {READ, WRITE};

    private final String methodName;
    private final ArrayList<Object> params;
    private final MethodType type;

    public MethodInvocationRequest(String methodName, MethodType type, ArrayList<Object> params) {
        this.methodName = methodName;
        this.type = type;
        this.params = params;
    }

    public ArrayList<Object> getParams() {
        return params;
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInvocationRequest)) return false;

        MethodInvocationRequest that = (MethodInvocationRequest) o;

        if (getMethodName() != null ? !getMethodName().equals(that.getMethodName()) : that.getMethodName() != null)
            return false;
        if (getParams() != null ? !getParams().equals(that.getParams()) : that.getParams() != null)
            return false;
        return getType() == that.getType();
    }

    @Override
    public int hashCode() {
        int result = getMethodName() != null ? getMethodName().hashCode() : 0;
        result = 31 * result + (getParams() != null ? getParams().hashCode() : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MethodInvocationRequest{" +
                "methodName='" + methodName + '\'' +
                ", params=" + params +
                ", type=" + type +
                '}';
    }
}
