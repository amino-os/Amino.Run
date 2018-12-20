package amino.run.oms;

import amino.run.app.NodeSelectorSpec;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.common.ReplicaID;
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

    Policy.GroupPolicy createGroupPolicy(Class<?> policyClass, MicroServiceID microServiceId)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    MicroServiceNotFoundException;

    void deleteGroupPolicy(MicroServiceID microServiceId, KernelOID groupOid)
            throws RemoteException, MicroServiceNotFoundException;

    MicroServiceID registerMicroService() throws RemoteException;

    ReplicaID registerReplica(MicroServiceID microServiceId)
            throws RemoteException, MicroServiceNotFoundException;

    void setReplicaDispatcher(ReplicaID replicaId, EventHandler dispatcher)
            throws RemoteException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException;

    boolean delete(MicroServiceID id) throws RemoteException, MicroServiceNotFoundException;

    void unRegisterMicroService(MicroServiceID microServiceId)
            throws RemoteException, MicroServiceNotFoundException;

    void unRegisterReplica(ReplicaID replicaId)
            throws RemoteException, MicroServiceNotFoundException;
}
