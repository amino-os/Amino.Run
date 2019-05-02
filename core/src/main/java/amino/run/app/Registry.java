package amino.run.app;

import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNameModificationException;
import amino.run.common.MicroServiceNotFoundException;
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
    MicroServiceID create(String spec, Object... args)
            throws RemoteException, MicroServiceCreationException;

    /**
     * Gets the application client side instance of microservice. It is used by client to do further
     * operations on the microservice.
     *
     * @param microServiceId
     * @return Application client side instance of microservice
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     */
    AppObjectStub acquireStub(MicroServiceID microServiceId)
            throws RemoteException, MicroServiceNotFoundException;

    /**
     * Attaches the client to an existing named microservice and returns the application client side
     * instance for it.
     *
     * @param name Name of the microservice to attach to
     * @return Application client side instance of microservice
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     */
    AppObjectStub attachTo(String name) throws RemoteException, MicroServiceNotFoundException;

    /**
     * Detaches the client from an existing named microservice.
     *
     * @apiNote Once detached, the client cannot make any further calls.
     * @param name Name of microservice to detach from
     * @return True or False indicating success or failure
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     */
    boolean detachFrom(String name) throws RemoteException, MicroServiceNotFoundException;

    /**
     * Assigns a name to a microservice.
     *
     * @apiNote It cannot be modified once set.
     * @param id
     * @param name
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     * @throws MicroServiceNameModificationException
     */
    void setName(MicroServiceID id, String name)
            throws RemoteException, MicroServiceNotFoundException,
                    MicroServiceNameModificationException;

    /**
     * Deletes a microservice.
     *
     * @apiNote Once deleted, clients cannot make any further calls.
     * @param id
     * @return True or False indicating success or failure
     * @throws RemoteException
     * @throws MicroServiceNotFoundException
     */
    boolean delete(MicroServiceID id) throws RemoteException, MicroServiceNotFoundException;
}
