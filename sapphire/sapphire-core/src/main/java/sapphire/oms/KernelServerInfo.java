package sapphire.oms;

import java.io.Serializable;
import java.net.InetSocketAddress;
import sapphire.kernel.server.KernelServer;

/**
 * This class is used to hold host address and its correspoding.
 * Created by Venugopal Reddy K 00900280 on 25/2/18.
 */

public class KernelServerInfo implements Serializable {
	InetSocketAddress host;
	KernelServer kernelServer;

	KernelServerInfo(InetSocketAddress hostAddr, KernelServer ks) {
		host = hostAddr;
		kernelServer = ks;
	}

	public InetSocketAddress getHost() { return host; }

	public KernelServer getKernelServer() { return kernelServer; }
}
