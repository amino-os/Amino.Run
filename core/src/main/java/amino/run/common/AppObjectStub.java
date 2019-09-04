package amino.run.common;

import amino.run.policy.Policy;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

public interface AppObjectStub extends Serializable, Cloneable {
    /**
     * Class to hold the APP context information. This information is passed along the RPC to remote
     * kernel server.
     */
    final class Context implements Serializable {
        public final UUID callerId; /* Caller Id identifying the client */
        public final InetSocketAddress host; /* Kernel server address on client */

        public Context(UUID calledId, InetSocketAddress host) {
            this.callerId = calledId;
            this.host = host;
        }
    }

    /* Thread local context to pass the client context from AppStub->DMClient->DMServerStub and make it available for
    makeKernelRPC() such that remote kernel server receives context information in KernelRPC, without having to pass
    context all along as argument to DMClient.onRPC() and DMServer.onRPC() for all DMs */
    ThreadLocal<Context> context = new ThreadLocal<Context>();

    public void $__initialize(MicroServiceID microServiceId, Policy.ClientPolicy client);

    public void $__initialize(boolean directInvocation);

    public MicroServiceID $__getMicroServiceId();

    public Object $__clone() throws CloneNotSupportedException;
}
