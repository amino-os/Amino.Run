package amino.run.common;

import amino.run.policy.Policy;
import java.io.Serializable;

public interface AppObjectStub extends Serializable, Cloneable {
    public void $__initialize(MicroServiceID microServiceId, Policy.ClientPolicy client);

    public void $__initialize(boolean directInvocation);

    public MicroServiceID $__getMicroServiceId();

    public Object $__clone() throws CloneNotSupportedException;
}
