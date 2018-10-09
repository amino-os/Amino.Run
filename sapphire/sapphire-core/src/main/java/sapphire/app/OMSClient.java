package sapphire.app;

import java.rmi.RemoteException;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;

/**
 * TODO(multi-lang): Implement OMSClient
 *
 * <p>Client used by applications to interact with OMS.
 *
 * <p>Ruby application: <code>
 *     oms = Java.type('sapphire.app.OMSClient')
 *     so = oms.getInstance().createSapphireObject(...)
 * </code>
 */
public class OMSClient {

    /** @return OMS client instance */
    public static OMSClient getInstance() {
        return null;
    }

    /**
     * Create sapphire object.
     *
     * @param sapphireObjectSpec sapphire object specification in YAML
     * @param args arguments to sapphire object constructor
     * @return ID of the newly created sapphire object
     * @throws RemoteException unable to reach OMS server
     * @throws SapphireObjectCreationException unable to create sapphire object.
     */
    public SapphireObjectID createSapphireObject(String sapphireObjectSpec, Object... args)
            throws RemoteException, SapphireObjectCreationException {
        return null;
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
        return null;
    }
}
