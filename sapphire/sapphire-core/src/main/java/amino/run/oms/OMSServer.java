package amino.run.oms;

import amino.run.app.NodeSelectorSpec;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.common.SapphireReplicaID;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelServerNotFoundException;
import amino.run.kernel.common.ServerInfo;
import amino.run.policy.Policy;
import amino.run.runtime.EventHandler;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

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

    Policy.GroupPolicy createGroupPolicy(Class<?> policyClass, SapphireObjectID sapphireObjId)
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
