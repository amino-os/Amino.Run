package amino.run.kernel.common;

import amino.run.policy.SapphirePolicy;
import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Kernel object stub. Basic mechanisms for stub to make RPC to a kernel object
 *
 * @author iyzhang
 */
public interface KernelObjectStub extends Serializable {
    public KernelOID $__getKernelOID();

    public InetSocketAddress $__getHostname();

    public void $__updateHostname(InetSocketAddress hostname);

    public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy);
}
