package sapphire.policy.scalability;

import java.net.InetSocketAddress;

import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectStub;

/**
 * Created by Vishwajeet on 2/4/18.
 */

public class Server_Stub extends LoadBalancedFrontendPolicy.ServerPolicy implements KernelObjectStub  {
    KernelOID $__oid = null;
    InetSocketAddress $__hostname = null;
    public Server_Stub(KernelOID oid) {
        this.oid = oid;
        this.$__oid = oid;
    }
    public KernelOID $__getKernelOID() {return $__oid;}
    public InetSocketAddress $__getHostname() {return $__hostname;}
    public void $__updateHostname(InetSocketAddress hostname) {this.$__hostname = hostname;}
}
