package sapphire.app;

import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;

import java.rmi.RemoteException;

/**
 * TODO(multi-lang):
 *
 * Client used by applications to interact with OMS.
 *
 * Ruby application:
 * <code>
 *     oms = Java.type('sapphire.app.OMSClient')
 *     so = oms.createSapphireObject(...)
 * </code>
 */
public class OMSClient {
    /**
     * Create sapphire object.
     *
     * @param sapphireObjectSpec sapphire object specification in YAML
     * @param args arguments to sapphire object constructor
     *
     * @return ID of the newly created sapphire object
     * @throws RemoteException unable to reach OMS server
     * @throws SapphireObjectCreationException unable to create sapphire object.
     */
    SapphireObjectID createSapphireObject(String sapphireObjectSpec, Object... args)
            throws RemoteException, SapphireObjectCreationException {
        return null;
    }

    /**
     * Acquire the reference to the sapphire object with the specifid ID.
     *
     * @param sapphireObjId sapphire object ID
     * @return sapphire object stub. Applications use stub to invoke methods
     * on remote sapphire objects.
     * @throws RemoteException unable to reach OMS server
     * @throws SapphireObjectNotFoundException unable to find a sapphire object
     * with the given ID
     */
    AppObjectStub acquireSapphireObjectStub(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException {
        return null;
    }
}
