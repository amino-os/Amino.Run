package sapphire.kernel.common;

import sapphire.kernel.client.KernelClient;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;

/**
 * Variables visible in the entire address space. They are set at
 * the node (nodeServer) creation:
 * 
 * nodeServer - reference to the Server of the node
 * nodeClient - reference to the Client of the node
 * 
 * The stubs use them as its easier and more transparent than passing
 * them as parameters.
 * 
 * @author aaasz
 *
 */

public class GlobalKernelReferences {
	public static KernelServerImpl nodeServer;
}