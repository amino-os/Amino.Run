package amino.run.app;

import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.SapphireObjectID;
import amino.run.common.MicroServiceNameModificationException;
import amino.run.common.SapphireObjectNotFoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/** Interface used by application client to interact with Amino.Run system. */
public interface Registry extends Remote {
    /**
     * Creates a microservice instance, deploys it on kernel server(s) and returns its Id.
     *
     * @param spec Specification of the microservice. See class MicroServiceSpec.
     * @param args Arguments to the constructor of the Microservice.
     * @return Id of the created microservice
     * @throws RemoteException
     * @throws MicroServiceCreationException
     */
    SapphireObjectID create(String spec, Object... args)
            throws RemoteException, MicroServiceCreationException;

    /**
     * Gets the application client side instance of microservice. It is used by client to do further
     * operations on the microservice.
     *
     * @param microServiceId
     * @return Application client side instance of microservice
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    AppObjectStub acquireStub(SapphireObjectID microServiceId)
            throws RemoteException, SapphireObjectNotFoundException;

    /**
     * Attaches the client to an existing named microservice and returns the application client side
     * instance for it.
     *
     * @param name Name of the microservice to attach to
     * @return Application client side instance of microservice
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    AppObjectStub attachTo(String name) throws RemoteException, SapphireObjectNotFoundException;

    /**
     * Detaches the client from an existing named microservice.
     *
     * @apiNote Once detached, the client cannot make any further calls.
     * @param name Name of microservice to detach from
     * @return True or False indicating success or failure
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    boolean detachFrom(String name) throws RemoteException, SapphireObjectNotFoundException;

    /**
     * Assigns a name to a microservice.
     *
     * @apiNote It cannot be modified once set.
     * @param id
     * @param name
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     * @throws MicroServiceNameModificationException
     */
    void setName(SapphireObjectID id, String name)
            throws RemoteException, SapphireObjectNotFoundException,
            MicroServiceNameModificationException;

    /**
     * Deletes a microservice.
     *
     * @apiNote Once deleted, clients cannot make any further calls.
     * @param id
     * @return True or False indicating success or failure
     * @throws RemoteException
     * @throws SapphireObjectNotFoundException
     */
    boolean delete(SapphireObjectID id) throws RemoteException, SapphireObjectNotFoundException;
}
