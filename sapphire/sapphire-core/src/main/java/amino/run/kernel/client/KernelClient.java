package amino.run.kernel.client;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectMigratingException;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.common.KernelObjectStubNotCreatedException;
import amino.run.kernel.common.KernelRPC;
import amino.run.kernel.common.KernelRPCException;
import amino.run.kernel.server.KernelObject;
import amino.run.kernel.server.KernelServer;
import amino.run.oms.OMSServer;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side object for making Sapphire kernel RPCs.
 *
 * @author iyzhang
 */
public class KernelClient {
    /** Stub for the OMS */
    private OMSServer oms;
    /** List of hostnames matched to kernel server stubs */
    private Hashtable<InetSocketAddress, KernelServer> servers;

    private Logger logger = Logger.getLogger(KernelClient.class.getName());

    /**
     * Add a host to the list of hosts that we've contacted
     *
     * @param host
     */
    private KernelServer addHost(InetSocketAddress host) {
        try {
            Registry registry = LocateRegistry.getRegistry(host.getHostName(), host.getPort());
            KernelServer server = (KernelServer) registry.lookup("SapphireKernelServer");
            servers.put(host, server);
            return server;
        } catch (Exception e) {
            logger.severe("Could not find Sapphire server on host: " + e.toString());
        }
        return null;
    }

    private KernelServer getServer(InetSocketAddress host) {
        KernelServer server = servers.get(host);
        if (server == null) {
            server = addHost(host);
        }
        return server;
    }

    public KernelClient(OMSServer oms) {
        this.oms = oms;
        servers = new Hashtable<InetSocketAddress, KernelServer>();
    }

    private Object tryMakeKernelRPC(KernelServer server, KernelRPC rpc)
            throws KernelObjectNotFoundException, Exception {
        Object ret = null;
        try {
            ret = server.makeKernelRPC(rpc);
        } catch (KernelRPCException e) {
            if (!(e.getException() instanceof InvocationTargetException)) {
                /* Not an invocation exception */
                throw e.getException();
            }

            /* Invocation target exception wraps exception thrown by an invoked method or constructor */
            /* If invocation exception is with any exception,including runtime, app exceptions, unwrap it
            and throw. Else it is invocation exception with error. Throw the invocation exception as is */
            Throwable cause = e.getException().getCause();
            if (cause instanceof InvocationTargetException) {
                cause = cause.getCause();
            }

            throw (cause instanceof Exception) ? ((Exception) cause) : e.getException();

        } catch (KernelObjectMigratingException e) {
            Thread.sleep(100);
            throw new KernelObjectNotFoundException(
                    "Kernel object was migrating. Try again later.");
        }
        return ret;
    }

    private Object lookupAndTryMakeKernelRPC(KernelObjectStub stub, KernelRPC rpc)
            throws KernelObjectNotFoundException, Exception {
        InetSocketAddress host, oldHost = stub.$__getHostname();

        try {
            host = oms.lookupKernelObject(stub.$__getKernelOID());
        } catch (RemoteException e) {
            throw new KernelObjectNotFoundException("Could not find oms.");
        } catch (KernelObjectNotFoundException e) {
            throw new KernelObjectNotFoundException("This object does not exist!");
        }

        if (host.equals(oldHost)) {
            throw new Error("Kernel object should be on the server!");
        }

        stub.$__updateHostname(host);
        return tryMakeKernelRPC(getServer(host), rpc);
    }

    /**
     * Make an RPC to the kernel server.
     *
     * @param stub
     * @param rpc
     * @return
     * @throws RemoteException when kernel server cannot be contacted
     * @throws KernelObjectNotFoundException when kernel server cannot find object
     */
    public Object makeKernelRPC(KernelObjectStub stub, KernelRPC rpc)
            throws KernelObjectNotFoundException, Exception {
        InetSocketAddress host = stub.$__getHostname();
        logger.log(Level.FINE, "Making RPC to " + host.toString() + " RPC: " + rpc.toString());

        // Check whether this object is local.
        KernelServer server;
        if (host.equals(GlobalKernelReferences.nodeServer.getLocalHost())) {
            server = GlobalKernelReferences.nodeServer;
        } else {
            server = getServer(host);
        }

        // Call the server
        try {
            return tryMakeKernelRPC(server, rpc);
        } catch (KernelObjectNotFoundException e) {
            logger.warning(
                    String.format(
                            "Object was not found at the target location. Host:%s oid:%d method:%s",
                            host.toString(), rpc.getOID().getID(), rpc.getMethod()));
            return lookupAndTryMakeKernelRPC(stub, rpc);
        }
    }

    public void copyObjectToServer(InetSocketAddress host, KernelOID oid, KernelObject object)
            throws RemoteException, KernelObjectNotFoundException,
                    KernelObjectStubNotCreatedException, MicroServiceNotFoundException,
                    MicroServiceReplicaNotFoundException {
        getServer(host).copyKernelObject(oid, object);
    }
}
