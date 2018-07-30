package sapphire.kernel.server;

import java.lang.annotation.Annotation;
import java.rmi.Remote;
import java.rmi.RemoteException;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.common.SapphireSoStub;
import sapphire.kernel.common.DMConfigInfo;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.policy.SapphirePolicy;
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
            throws RemoteException, KernelObjectNotFoundException;

    // AppObjectStub startApp(String className) throws RemoteException;

    SapphireSoStub createSapphireObject(String className, Object... args) throws RemoteException, SapphireObjectCreationException, ClassNotFoundException;

    SapphireSoStub createSapphireObject(
            String className, String runtimeType, String constructorName, byte[] args)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    InstantiationException, KernelObjectNotFoundException,
                    SapphireObjectNotFoundException, IllegalAccessException;

    void deleteSapphireObject(SapphireObjectID sapphireObjId, EventHandler handler)
            throws RemoteException, SapphireObjectNotFoundException;

    void deleteSapphireReplica(SapphireReplicaID sapphireReplicaId, EventHandler handler)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException;

    SapphirePolicy.SapphireClientPolicy createSapphireClientPolicy(
            String sapphireClientPolicy,
            SapphirePolicy.SapphireServerPolicy serverPolicy,
            SapphirePolicy.SapphireGroupPolicy groupPolicy,
            Annotation[] annotations)
            throws RemoteException, IllegalAccessException, InstantiationException,
                    ClassNotFoundException, KernelObjectNotCreatedException;

    void deleteSapphireClientPolicy(KernelOID oid)
            throws RemoteException, KernelObjectNotFoundException;

    SapphireReplicaID createInnerSapphireObject(
            String className, DMConfigInfo dm, SapphireObjectID parentSid, byte[] objectStream)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException,
                    ClassNotFoundException, KernelObjectNotCreatedException, InstantiationException,
                    IllegalAccessException, SapphireObjectCreationException;

    boolean deleteInnerSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;
}
