package sapphire.app;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.graalvm.polyglot.Value;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.graal.io.SerializeValue;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;

/**
 * TODO(multi-lang): Implement OMSClient
 *
 * <p>Client used by applications to interact with OMS.
 *
 * <p>Ruby application: <code>
 *      omsClient = Java.type('sapphire.app.OMSClient').new(host, oms)
 *      so = omsClient.createSapphireObject(...)
 * </code>
 */
public class OMSClient {
    private SapphireObjectServer server;

    /**
     * Create OMS client object.
     *
     * @param host HOST InetSocketAddress
     * @param omsHost OMS InetSocketAddress
     * @throws RemoteException unable to reach OMS server
     * @throws NotBoundException oms server not bind with "SapphireOMS" name
     */
    public OMSClient(InetSocketAddress host, InetSocketAddress omsHost)
            throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(omsHost.getHostName(), omsHost.getPort());
        server = (SapphireObjectServer) registry.lookup("SapphireOMS");
        KernelServer nodeServer = new KernelServerImpl(host, omsHost);
    }
    /**
     * Create sapphire object.
     *
     * @param spec sapphire object specification in YAML
     * @param args arguments to sapphire object constructor
     * @return ID of the newly created sapphire object
     * @throws RemoteException unable to reach OMS server
     * @throws SapphireObjectCreationException unable to create sapphire object.
     */
    public SapphireObjectID createSapphireObject(SapphireObjectSpec spec, Object... args)
            throws RemoteException, SapphireObjectCreationException {

        // convert graal...Value to serializable object
        Object[] serializableObjects = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof org.graalvm.polyglot.Value) {
                try {
                    serializableObjects[i] =
                            SerializeValue.getSerializeValue((Value) args[i], spec.getLang());
                } catch (Exception e) {
                    // TODO: Add Serialization exception for
                    // sapphire.graal.Serialization/Deserialization
                    // currently sending SapphireObjectCreationException
                    throw new SapphireObjectCreationException("Failed to serialize");
                }
                continue;
            }
            serializableObjects[i] = args[i];
        }
        return server.createSapphireObject(spec.toString(), serializableObjects);
    }

    /**
     * Acquire the reference to the sapphire object with the specifid ID.
     *
     * @param sapphireObjId sapphire object ID
     * @return sapphire object stub. Applications use stub to invoke methods on remote sapphire
     *     objects.
     * @throws RemoteException unable to reach OMS server
     * @throws SapphireObjectNotFoundException unable to find a sapphire object with the given ID
     */
    public AppObjectStub acquireSapphireObjectStub(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        return server.acquireSapphireObjectStub(sapphireObjId);
    }
}
