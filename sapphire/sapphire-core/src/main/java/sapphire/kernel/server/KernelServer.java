package sapphire.kernel.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStubNotCreatedException;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.runtime.EventHandler;

/**
 * Interface for the Sapphire Kernel Server
 *
 * @author iyzhang
 */
public interface KernelServer extends Remote {
    Object makeKernelRPC(KernelRPC rpc)
            throws RemoteException, KernelObjectNotFoundException, KernelObjectMigratingException,
                    KernelRPCException;

    void copyKernelObject(KernelOID oid, KernelObject object)
            throws RemoteException, KernelObjectNotFoundException,
                    KernelObjectStubNotCreatedException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException;

    AppObjectStub createSapphireObject(String className, Object... args)
            throws RemoteException, SapphireObjectCreationException, ClassNotFoundException;

    void deleteSapphireObjectHandler(EventHandler handler) throws RemoteException;

    void deleteSapphireReplicaHandler(EventHandler handler) throws RemoteException;
}
