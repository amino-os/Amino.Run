package amino.run.app;

import amino.run.common.AppObjectStub;
import amino.run.common.SapphireObjectCreationException;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireObjectNameModificationException;
import amino.run.common.SapphireObjectNotFoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Interface used by Application client to interact with DCAP Sapphire system. */
public interface SapphireObjectServer extends Remote {
    /**
     * Creates a sapphire object, deploys it on kernel server(s) and returns the sapphire object Id.
     *
     * @param sapphireObjectSpec
     * @param args
     * @return Sapphire Object Id
     * @throws RemoteException
     * @throws SapphireObjectCreationException
     */
    SapphireObjectID createSapphireObject(String sapphireObjectSpec, Object... args)
            throws RemoteException, SapphireObjectCreationException;

    /**
     * Gets the application client side instance of sapphire object. It is used by client to do
     * further operations on the sapphire object.
     *
     * @param sapphireObjId
     * @return Application client side instance for Sapphire object
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    AppObjectStub acquireSapphireObjectStub(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    /**
     * Attaches the client to the existing sapphire object in the Sapphire System and returns the
     * application client side instance for it.
     *
     * @param sapphireObjName
     * @return Application client side instance for Sapphire object
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    AppObjectStub attachToSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException;

    /**
     * Detaches the client from the existing sapphire object in the Sapphire System.
     *
     * @apiNote Once detached, cannot make any calls on application client side instance of sapphire
     *     object.
     * @param sapphireObjName
     * @return True or False indicating success or failure
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    boolean detachFromSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException;

    /**
     * Assigns a name to the Sapphire object.
     *
     * @apiNote It cannot be modified once set.
     * @param sapphireObjId
     * @param sapphireObjName
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     * @throws SapphireObjectNameModificationException
     */
    void setSapphireObjectName(SapphireObjectID sapphireObjId, String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectNameModificationException;

    /**
     * Deletes the sapphire object.
     *
     * @apiNote Once deleted, cannot make any calls on application client side instance of sapphire
     *     object.
     * @param sapphireObjId
     * @return True or False indicating success or failure
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    boolean deleteSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;
}
