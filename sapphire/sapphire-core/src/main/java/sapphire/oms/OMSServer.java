package sapphire.oms;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import sapphire.app.NodeSelectorSpec;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelServerNotFoundException;
import sapphire.kernel.common.ServerInfo;
import sapphire.policy.SapphirePolicy;
import sapphire.runtime.EventHandler;

public interface OMSServer extends Remote {
    public static final long KS_HEARTBEAT_TIMEOUT = 6000; // milliseconds

    KernelOID registerKernelObject(InetSocketAddress host) throws RemoteException;

    void registerKernelObject(KernelOID oid, InetSocketAddress host)
            throws RemoteException, KernelObjectNotFoundException;

    void unRegisterKernelObject(KernelOID oid, InetSocketAddress host)
            throws RemoteException, KernelObjectNotFoundException;

    InetSocketAddress lookupKernelObject(KernelOID oid)
            throws RemoteException, KernelObjectNotFoundException;

    ArrayList<String> getRegions() throws RemoteException;

    List<InetSocketAddress> getServers(NodeSelectorSpec spec) throws RemoteException;

    void registerKernelServer(ServerInfo info) throws RemoteException, NotBoundException;

    void heartbeatKernelServer(ServerInfo srvinfo)
            throws RemoteException, NotBoundException, KernelServerNotFoundException;

    SapphirePolicy.SapphireGroupPolicy createGroupPolicy(
            Class<?> policyClass, SapphireObjectID sapphireObjId)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException;

    SapphireObjectID registerSapphireObject() throws RemoteException;

    SapphireReplicaID registerSapphireReplica(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    void setSapphireReplicaDispatcher(SapphireReplicaID replicaId, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException;

    boolean deleteSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    void unRegisterSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    void unRegisterSapphireReplica(SapphireReplicaID replicaId)
            throws RemoteException, SapphireObjectNotFoundException;
}
