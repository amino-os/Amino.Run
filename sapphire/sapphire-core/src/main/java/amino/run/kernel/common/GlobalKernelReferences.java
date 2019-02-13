package amino.run.kernel.common;

import amino.run.kernel.server.KernelServerImpl;

/**
 * Variables visible in the entire address space. They are set at the node (nodeServer) creation:
 *
 * <p>nodeServer - reference to the Server of the node nodeClient - reference to the Client of the
 * node
 *
 * <p>The stubs use them as its easier and more transparent than passing them as parameters.
 *
 * @author aaasz
 */
public class GlobalKernelReferences {
    public static KernelServerImpl nodeServer;
}
