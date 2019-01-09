package amino.run.common;

import amino.run.policy.SapphirePolicy.SapphireClientPolicy;
import java.io.Serializable;

public interface AppObjectStub extends Serializable, Cloneable {
    public void $__initialize(SapphireClientPolicy client);

    public void $__initialize(boolean directInvocation);

    public Object $__clone() throws CloneNotSupportedException;
}
