package sapphire.oms;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNameModificationException;
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

    ArrayList<InetSocketAddress> getServers()
            throws NumberFormatException, RemoteException, NotBoundException;

    ArrayList<String> getRegions() throws RemoteException;

    InetSocketAddress getServerInRegion(String region) throws RemoteException;

    ArrayList<InetSocketAddress> getServersInRegion(String region) throws RemoteException;

    void registerKernelServer(ServerInfo info) throws RemoteException, NotBoundException;

    void heartbeatKernelServer(ServerInfo srvinfo)
            throws RemoteException, NotBoundException, KernelServerNotFoundException;

    SapphirePolicy.SapphireGroupPolicy createGroupPolicy(
            Class<?> policyClass, SapphireObjectID sapphireObjId, Annotation[] appConfigAnnotation)
            throws RemoteException, ClassNotFoundException, KernelObjectNotCreatedException,
                    SapphireObjectNotFoundException;

    SapphireObjectID registerSapphireObject() throws RemoteException;

    SapphireReplicaID registerSapphireReplica(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    void setSapphireObjectDispatcher(SapphireObjectID sapphireObjId, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException;

    void setSapphireReplicaDispatcher(SapphireReplicaID replicaId, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException;

    EventHandler getSapphireObjectDispatcher(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    EventHandler getSapphireReplicaDispatcher(SapphireReplicaID replicaId)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectReplicaNotFoundException;

    SapphireObjectID createSapphireObject(String absoluteSapphireClassName, Object... args)
            throws RemoteException, SapphireObjectCreationException;

    AppObjectStub acquireSapphireObjectStub(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    AppObjectStub attachToSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException;

    boolean detachFromSapphireObject(String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException;

    void setSapphireObjectName(SapphireObjectID sapphireObjId, String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException,
                    SapphireObjectNameModificationException;

    boolean deleteSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    void unRegisterSapphireObject(SapphireObjectID sapphireObjId)
            throws RemoteException, SapphireObjectNotFoundException;

    void unRegisterSapphireReplica(SapphireReplicaID replicaId)
            throws RemoteException, SapphireObjectNotFoundException;

    // testing purpose
    ArrayList<SapphireObjectID> getAllSapphireObjects() throws RemoteException;

    EventHandler[] getSapphireReplicasById(SapphireObjectID oid)
            throws RemoteException, SapphireObjectNotFoundException;

    ArrayList<KernelOID> getAllKernelObjects() throws RemoteException;
}
