package amino.run.common;

import amino.run.policy.Policy;
import java.io.Serializable;

public interface AppObjectStub extends Serializable, Cloneable {
    public static final ThreadLocal<AppContext> context = new ThreadLocal<>();

    public void $__initialize(Policy.ClientPolicy client);

    public void $__initialize(boolean directInvocation);

    public Object $__clone() throws CloneNotSupportedException;
}
