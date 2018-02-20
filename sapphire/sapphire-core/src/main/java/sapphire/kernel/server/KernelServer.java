package sapphire.kernel.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import sapphire.common.AppObjectStub;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;

/** 
 * Interface for the Sapphire Kernel Server
 * 
 * @author iyzhang
 *
 */
public interface KernelServer extends Remote {
	Object makeKernelRPC(KernelRPC rpc) throws RemoteException, KernelObjectNotFoundException, KernelObjectMigratingException, KernelRPCException;
	void copyKernelObject(KernelOID oid, KernelObject object) throws RemoteException, KernelObjectNotFoundException;
    void createSapphireObjectReplica(String serverPolicyName, String groupPolicyName, KernelOID groupOid, AppObjectStub appObjectStub) throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException, KernelObjectNotFoundException;
	AppObjectStub startApp(String className) throws RemoteException;
}
