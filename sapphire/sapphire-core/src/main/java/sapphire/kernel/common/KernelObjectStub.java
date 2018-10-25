package sapphire.kernel.common;

import java.io.Serializable;
import java.net.InetSocketAddress;
import sapphire.policy.SapphirePolicy;

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

    public SapphirePolicy.SapphireClientPolicy $__getNextClientPolicy();
}
