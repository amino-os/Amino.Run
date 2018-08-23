package sapphire.kernel.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
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

    // AppObjectStub startApp(String className) throws RemoteException;

    AppObjectStub createSapphireObject(String className, Object... args)
            throws RemoteException, SapphireObjectCreationException, ClassNotFoundException;

    void deleteSapphireObjectHandler(SapphireObjectID sapphireObjId, EventHandler handler)
            throws RemoteException;

    void deleteSapphireReplicaHandler(SapphireReplicaID sapphireReplicaId, EventHandler handler)
            throws RemoteException;
}
