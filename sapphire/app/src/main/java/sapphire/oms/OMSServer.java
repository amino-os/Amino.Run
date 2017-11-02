package sapphire.oms;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.runtime.EventHandler;
import sapphire.common.AppObjectStub;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireReplicaID;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectNotFoundException;

public interface OMSServer extends Remote {
       KernelOID registerKernelObject(InetSocketAddress host) throws RemoteException;
       void registerKernelObject(KernelOID oid, InetSocketAddress host) throws RemoteException, KernelObjectNotFoundException;
       InetSocketAddress lookupKernelObject(KernelOID oid) throws RemoteException, KernelObjectNotFoundException;
       
       ArrayList<InetSocketAddress> getServers() throws NumberFormatException, RemoteException, NotBoundException;
       ArrayList<String> getRegions() throws RemoteException;
       InetSocketAddress getServerInRegion(String region) throws RemoteException;
       
       void registerKernelServer(InetSocketAddress host) throws RemoteException, NotBoundException;
       
       SapphireObjectID registerSapphireObject(EventHandler dispatcher) throws RemoteException;
       SapphireReplicaID registerSapphireReplica(SapphireObjectID oid, EventHandler dispatcher) throws RemoteException, SapphireObjectNotFoundException;
       EventHandler getSapphireObjectDispatcher(SapphireObjectID oid) throws RemoteException, SapphireObjectNotFoundException;
       EventHandler getSapphireReplicaDispatcher(SapphireReplicaID rid) throws RemoteException, SapphireObjectNotFoundException;
       
       /* Called by the client */
       public AppObjectStub getAppEntryPoint() throws RemoteException;
}

