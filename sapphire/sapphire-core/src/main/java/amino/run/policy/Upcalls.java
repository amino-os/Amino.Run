package amino.run.policy;

import amino.run.app.MicroServiceSpec;
import amino.run.runtime.MicroService;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Upcall interface used by sapphire kernel to invoke event handlers on client, server and group
 * policies//deployment managers. Note that in the MicroService paper these are discussed and
 * referred to variously as the "Deployment Managers Upcall API" (in general), and "proxy" (for
 * client), "instance manager" (for server) and "coordinator" (for group). These methods should only
 * be invoked by the kernel, not by DM's (other than when extending default/inherited
 * implementations), and not by applications. TODO: Restructure code so that it's not possible/easy
 * for these to be involked from the wrong places, as is for example currently done in
 * ConsensusRSMPolicy.
 */
public interface Upcalls {

    /**
     * Interface for sapphire policy configuration.
     *
     * <p>Each sapphire policy can optionally define a Config class to allow programmers to pass
     * configurations to the sapphire policy. All Config classes should implement this interface.
     * TODO: Quinton: This does not belong in Upcalls interface. Move it to a more appropriate
     * place.
     */
    interface SapphirePolicyConfig extends Serializable {}

    /**
     * Interface for client policy. These are the methods invoked by the sapphire kernel against all
     * client policies to handle events.
     */
    interface ClientUpcalls extends Serializable {
        /**
<<<<<<< HEAD
         * Event handler for microservice creation. Called after a primary microservice is first
         * created (i.e. before any replicas are created). This is called after {@link setServer}
         * (currently in {@link MicroService.createPolicy} and before {@link
=======
         * Event handler for microservice creation. Called after a primary microservice is
         * first created (i.e. before any replicas are created). This is called after {@link
         * setServer} (currently in {@link MicroService.createPolicy} and before {@link
>>>>>>> Replace 'sapphire object' with 'microservice' throughout.
         * ServerUpcalls.onCreate}. It is usually used to store a reference to the group policy for
         * this SO, and to initialize the client. Configuration parameters for the client are
         * contained in spec.
         *
         * @param group the group policy for the newly created microservice.
         * @param spec microservice spec, which contains configuration parameters for the client
         *     TODO: Check that there really are configuration parameters for the client buried in
         *     there, and also change this parameter to more specifically be client configuration
         *     parameters.
         */
        void onCreate(Policy.GroupPolicy group, MicroServiceSpec spec);

        /**
         * Event handler for remote procedure calls/method invocations against the microservice.
         * Usually the default handling is simply to invoke onRPC against the remote server policy
         * of the cached primary replica. But DM's may over-ride this to direct the call to other
         * replicas, retry on failure, etc. NOTE: Strictly speaking this is not an upcall from the
         * DK in the current implementation. onRPC() is invoked directly from the appStub, which is
         * invoked by the app client.
         *
         * @param method The name of the method to be invoked.
         * @param params the parameters to be passed to the remote method invocation. All parameter
         *     passing is by definition pass-by-value, because it's remote.
         * @return the return value from the remote method invocation. Again, return values are
         *     pass-by-value.
         * @throws Exception Either the exception thrown directly by the remote method invocation,
         *     or an exception thrown by the sapphire kernel or network stack.
         */
        Object onRPC(String method, ArrayList<Object> params) throws Exception;
    }

    /** Interface for server policy. */
    interface ServerUpcalls extends Serializable {

        /**
         * Event handler for sapphire replica creation. Called after a sapphire replica (including
         * the first one) is first created. This is called after {@link ClientUpcalls.onCreate}
         * (currently in {@link MicroService.createPolicy} and before {@link GroupUpcalls.onCreate}
         * and before {@link GroupUpcalls.addServer}. It is usually used to store a reference to the
         * group policy for this SO, and to otherwise initialize the server. Configuration
         * parameters for the server are contained in spec.
         *
         * @param group the group policy for the newly created microservice.
         * @param spec microservice spec, which contains configuration parameters for the server
         *     TODO: Check that there really are configuration parameters for the server buried in
         *     there, and also change this parameter to more specifically be server configuration
         *     parameters.
         */
        void onCreate(Policy.GroupPolicy group, MicroServiceSpec spec);

        /**
<<<<<<< HEAD
         * Event handler for microservice destruction. Called immediately before a sapphire object
         * (or replica) is deleted from a kernel server. Usually used to tear down a server policy's
         * local resources, for example, timers, network connections, etc. Currently called in
         * {@link KernelServerImpl.deleteKernelObject} and {@link
=======
         * Event handler for microservice destruction. Called immediately before a sapphire
         * object (or replica) is deleted from a kernel server. Usually used to tear down a server
         * policy's local resources, for example, timers, network connections, etc. Currently called
         * in {@link KernelServerImpl.deleteKernelObject} and {@link
>>>>>>> Replace 'sapphire object' with 'microservice' throughout.
         * KernelServerImpl.moveKernelObjectToServer} (on the old server, after moving the object to
         * the new server). TODO: Quinton: It's not clear to my why the latter call is needed.
         * Surely moveKernelObjectToServer should just call deleteKernelObject on the old server?
         * Venu: Yes. Apart from that, deleteKernelObject unregisters kernel object from OMS too.
         * Upon move kernel object, we need to avoid unregister kernel object from OMS. We may have
         * a common method deleteLocalKernelObject() which just does same as deleteKernelObject
         * without unregister part. And call the same deleteLocalKernelObject() from both
         * moveKernelObjectToServer() and deleteKernelObject()
         */
        void onDestroy();

        /**
         * Get a cached reference to the remote group policy running on OMS.
         *
         * @return The group policy for this sapphire replica.
         */
        Policy.GroupPolicy getGroup();

        /**
         * Event handler for remote procedure calls/method invocations against this replica of the
         * microservice. Invoked by every client policy/DM (indirectly, via the sapphire kernel).
         * The standard implementation is simply to invoke the method on the local microservice
         * replica. May be overidden in DM's to, for example, replicate the call to all replicas,
         * detect and handle server overloads, persist transaction logs, etc.
         *
         * @param method Name of the method to be invoked.
         * @param params Parameters to be passed to the method invocation.
         * @return
         * @throws Exception Either the exception thrown by the application, or an exception
         *     resulting from the operations of the DM/Kernel itself (e.g. if RPC replication fails,
         *     server is overloaded, etc. TODO: Quinton: make it clearer which type of exception is
         *     being thrown. Application exceptions should be wrapped and sent back to the client DM
         *     (which may handle them), or just pass them straight back ot the client application.
         *     So ideally we explicitly return either a wrapped application exception, or one of an
         *     explicit set of kernel or DM exceptions.
         */
        Object onRPC(String method, ArrayList<Object> params) throws Exception;

        /**
         * Event handler to notify replicas of an SO that the set of replicas of this SO have
         * changed. Usually used to refresh the locally cached set of replicas (if the DM keeps
         * one), by remotely calling the group policy on OMS to determine the new set of replicas.
         * TODO: Quinton: Curiously, as far as I can tell, this is never invoked anywhere by the
         * sapphire kernel, or from anywhere else. It's also not implemented anywhere either, other
         * than no-op implementations to satisfy this interface. We need to fix this to invoke htis
         * method correctly from the sapphire kernel every time a replica is created, destroyed or
         * moved. TODO: Another todo. Why do we not just pass the new list of members in here as a
         * parameter, to avoid the DM having to make another round trip to the OMS to fetch the new
         * list? Perhaps that will be too expensive and we can leave that until later, if at all. In
         * which case onMembershipChange() can be used to cimply invalidate the cache. TODO: Also,
         * this is probably not actually invoked by the DK, but rather by the GroupPolicy. So it
         * should be moved to a different interface than this one.
         */
        void onMembershipChange();
    }

    interface GroupUpcalls extends Serializable {
        /**
         * Event handler for microservice creation. Called by the sapphire kernel on creation of
         * this group and it's first/primary replica. DM's may implement this method to initialize
         * the group policy, create additional replicas, etc. Currently this is invoked from {@link
         * Sapphire.createPolicy} during construction (new_ or create).
         *
         * @param region TODO: Quinton: This parameter is deprecated and must be deleted.
         * @param server reference to the server policy of the first/primary replica that is managed
         *     by the group policy
<<<<<<< HEAD
         * @param spec microservice spec. This contains configuration parameters that may be used to
         *     configure this group policy.
=======
         * @param spec microservice spec. This contains configuration parameters that may be used
         *     to configure this group policy.
>>>>>>> Replace 'sapphire object' with 'microservice' throughout.
         */
        void onCreate(String region, Policy.ServerPolicy server, MicroServiceSpec spec)
                throws RemoteException;

        /**
<<<<<<< HEAD
         * Event handler for microservice destruction. Called immediately before the group policy is
         * deleted, as part of object deletion. Usually used to tear down a group policy's local
=======
         * Event handler for microservice destruction. Called immediately before the group policy
         * is deleted, as part of object deletion. Usually used to tear down a group policy's local
>>>>>>> Replace 'sapphire object' with 'microservice' throughout.
         * resources, for example, timers, network connections, etc. Currently called by
         * InstanceManager.clear, which is called by MicroServiceManager.removeInstance.
         *
         * @throws RemoteException
         */
        void onDestroy() throws RemoteException;

        /**
         * Get all the replicas in this group. TODO: This does not belong in the upcall interface.
         * It is invoked by ServerPolicy and ClientPolicy to discover the members of the group.
         *
         * @return References to server policies of all replicas in this group.
         * @throws RemoteException
         */
        ArrayList<Policy.ServerPolicy> getServers() throws RemoteException;

        /**
         * Event handler for when replicas fail. TODO: Quinton: This is currently not called from
         * anywhere. Once replica monitoring has been implemented, this should be called for failed
         * replicas. Until then, remove it to avoid confusion.
         *
         * @param server Reference to the server policy of the failed replica (my be unresponsive).
         * @throws RemoteException
         */
        // void onFailure(ServerPolicy server) throws RemoteException;

        Policy.ServerPolicy onRefRequest() throws RemoteException;
    }
}
