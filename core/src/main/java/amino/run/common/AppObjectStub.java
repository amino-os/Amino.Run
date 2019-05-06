package amino.run.common;

import amino.run.policy.Policy;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

public interface AppObjectStub extends Serializable, Cloneable {
    final class Context implements Serializable {
        public final UUID callerId;
        public final InetSocketAddress host;

        public Context(UUID calledId, InetSocketAddress host) {
            this.callerId = calledId;
            this.host = host;
        }
    }

    ThreadLocal<Context> context = new ThreadLocal<Context>();

    public void $__initialize(MicroServiceID microServiceId, Policy.ClientPolicy client);

    public void $__initialize(boolean directInvocation);

    public MicroServiceID $__getMicroServiceId();

    public Object $__clone() throws CloneNotSupportedException;
}
