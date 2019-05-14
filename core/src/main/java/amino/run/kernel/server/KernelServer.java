package amino.run.kernel.server;

import amino.run.app.MicroServiceSpec;
import amino.run.common.AppObjectStub;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectMigratingException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStubNotCreatedException;
import amino.run.kernel.common.KernelRPC;
import amino.run.kernel.common.KernelRPCException;
import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface for the MicroService Kernel Server
 *
 * @author iyzhang
 */
public interface KernelServer extends Remote {
    Object makeKernelRPC(KernelRPC rpc)
            throws RemoteException, KernelObjectNotFoundException, KernelObjectMigratingException,
                    KernelRPCException;

    void copyKernelObject(KernelOID oid, KernelObject object)
            throws RemoteException, KernelObjectNotFoundException,
                    KernelObjectStubNotCreatedException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException;

    /**
     * Updates a kernel server about the list of all available kernel servers managed by the OMS
     *
     * @param serverList
     * @throws RemoteException
     */
    void updateAvailableKernelServers(List<InetSocketAddress> serverList) throws RemoteException;

    /**
     * Receive HeartBeat with random bytes to measure data transfer time
     *
     * @param randomBytes
     * @throws RemoteException
     */
    void receiveHeartBeat(byte[] randomBytes) throws RemoteException;

    /**
     * Receive HeartBeat with empty data to measure latency between kernel servers
     *
     * @throws RemoteException
     */
    void receiveHeartBeat() throws RemoteException;

    /**
     * Create microservice in kernel server
     *
     * @param spec {@link MicroServiceSpec} instance.
     * @param args parameters to microservice constructor
     * @return microservice stub
     * @throws RemoteException
     * @throws MicroServiceCreationException
     * @throws ClassNotFoundException
     */
    AppObjectStub createMicroService(MicroServiceSpec spec, Object... args)
            throws RemoteException, MicroServiceCreationException, ClassNotFoundException;
}
