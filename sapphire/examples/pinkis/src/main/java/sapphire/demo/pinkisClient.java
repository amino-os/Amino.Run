package sapphire.demo;

import sapphire.common.SapphireObjectCreationException;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class pinkisClient {
    public static void main(String[] args) {
        System.out.println("hello demo");

        // pinkisServer server = new pinkisServer();

        pinkisServer server = null;

        try {
            KernelServer dummyLocalKS = new KernelServerImpl(new InetSocketAddress("127.0.0.2", 11111), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

            Registry registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            OMSServer omsserver = (OMSServer) registry.lookup("SapphireOMS");
            SapphireObjectID soid = omsserver.createSapphireObject("sapphire.demo.pinkisServer");
            server = (pinkisServer)omsserver.acquireSapphireObjectStub(soid);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (SapphireObjectCreationException e) {
            e.printStackTrace();
        } catch (SapphireObjectNotFoundException e) {
            e.printStackTrace();
        }

        server.set("foo", "bar");
        Serializable value = server.get("foo");
        System.out.println("expecting: bar; got: " + value);
    }
}
