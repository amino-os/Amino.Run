package sapphire.oms;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.common.SapphireSoStub;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotCreatedException;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelServerNotFoundException;
import sapphire.kernel.common.ServerInfo;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
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

    SapphireGroupPolicy createGroupPolicy(Class<?> policyClass, SapphireObjectID oid)
            throws RemoteException, KernelObjectNotCreatedException, KernelObjectNotFoundException,
                    ClassNotFoundException, SapphireObjectNotFoundException;

    SapphireObjectID registerSapphireObject() throws RemoteException;

    SapphireReplicaID registerSapphireReplica(SapphireObjectID oid, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException;

    EventHandler getSapphireObjectDispatcher(SapphireObjectID oid)
            throws RemoteException, SapphireObjectNotFoundException;

    EventHandler getSapphireReplicaDispatcher(SapphireReplicaID rid)
            throws RemoteException, SapphireObjectNotFoundException;

    void setSapphireObjectHandler(SapphireObjectID oid, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException;

    void setSapphireReplicaHandler(SapphireReplicaID rid, EventHandler dispatcher)
            throws RemoteException, SapphireObjectNotFoundException;

    void unRegisterSapphireObject(SapphireObjectID oid)
            throws RemoteException, SapphireObjectNotFoundException;

    void unRegisterSapphireReplica(SapphireReplicaID rid)
            throws RemoteException, SapphireObjectNotFoundException;
    /* Called by the client */
    // AppObjectStub getAppEntryPoint() throws RemoteException;

    SapphireObjectID createSapphireObject(String absoluteSapphireClassName, Object... args) throws RemoteException, SapphireObjectCreationException, ClassNotFoundException;
    AppObjectStub acquireSapphireObjectStub(SapphireObjectID sapphireObjId) throws RemoteException, SapphireObjectNotFoundException;
    boolean releaseSapphireObjectStub(SapphireObjectID sapphireObjId) throws RemoteException, SapphireObjectNotFoundException;
    AppObjectStub attachToSapphireObject(String sapphireObjName) throws RemoteException, SapphireObjectNotFoundException;
    boolean detachFromSapphireObject(String sapphireObjName) throws RemoteException, SapphireObjectNotFoundException;

    SapphireObjectID createSapphireObject(
            String absoluteSapphireClassName,
            String runtimeType,
            String constructorName,
            byte[] args)
            throws RemoteException, SapphireObjectCreationException,
                    KernelObjectNotCreatedException, InstantiationException,
                    KernelObjectNotFoundException, IllegalAccessException, ClassNotFoundException;

    void setSapphireObjectName(SapphireObjectID sapphireObjectID, String sapphireObjName)
            throws RemoteException, SapphireObjectNotFoundException;

    SapphireClientInfo acquireSapphireObjectStub(
            SapphireObjectID sapphireObjectID, InetSocketAddress clientPolicyKernelServerHost)
            throws RemoteException, SapphireObjectNotFoundException, ClassNotFoundException,
                    KernelObjectNotCreatedException, InstantiationException, IllegalAccessException;

    boolean releaseSapphireObjectStub(SapphireObjectID sapphireObjId, int clientId)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException;

    SapphireClientInfo attachToSapphireObject(
            String sapphireObjName, InetSocketAddress clientPolicyKernelServerHost)
            throws RemoteException, SapphireObjectNotFoundException, ClassNotFoundException,
                    KernelObjectNotCreatedException, InstantiationException, IllegalAccessException;

    boolean detachFromSapphireObject(SapphireObjectID sapphireObjId, int clientId)
            throws RemoteException, SapphireObjectNotFoundException, KernelObjectNotFoundException;

    boolean deleteSapphireObject(SapphireObjectID sapphireObjectID)
            throws RemoteException, SapphireObjectNotFoundException;

    void setInnerSapphireObject(SapphireSoStub soStub) throws RemoteException, SapphireObjectNotFoundException;
}
