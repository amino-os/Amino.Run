package sapphire.common;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

import sapphire.kernel.client.KernelClient;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.KernelServerManager;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;

import static org.mockito.Mockito.spy;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.common.UtilsTest.setFieldValueOnInstance;

/**
 * Created by Vishwajeet on 4/4/18.
 */

public class SapphireUtils {
    static int omsPort = 10000;

    public static OMSServerImpl startSpiedOms(String appEntryClassName) throws Exception {
        Registry registry;
        OMSServerImpl myOms = new OMSServerImpl(appEntryClassName);
        KernelServerManager serverManager = (KernelServerManager)extractFieldValueOnInstance(myOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(myOms, "serverManager", spiedServerManager);
        OMSServerImpl spiedOms = spy(myOms);
        OMSServer omsStub = (OMSServer) UnicastRemoteObject.exportObject(spiedOms, 0);

        try {
            registry = LocateRegistry.createRegistry(omsPort);
        } catch (ExportException e) {
            registry = LocateRegistry.getRegistry(omsPort);
        } catch (RemoteException e) {
            return null;
        }

        registry.rebind("SapphireOMS", omsStub);
        return spiedOms;
    }

    public static KernelServerImpl startSpiedKernelServer(OMSServerImpl spiedOms, int port, String region) throws Exception {
        Registry registry;
        KernelServerImpl ks = new KernelServerImpl(new InetSocketAddress("127.0.0.1", port),
                new InetSocketAddress("127.0.0.1", omsPort));
        KernelClient kernelClient = (KernelClient) extractFieldValueOnInstance(ks, "client");
        KernelClient spiedKernelClient = spy(kernelClient);
        setFieldValueOnInstance(ks, "client", spiedKernelClient);
        ks.oms = spiedOms;
        KernelServerImpl spiedKs = spy(ks);
        KernelServer stub = (KernelServer) UnicastRemoteObject.exportObject(spiedKs, 0);

        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (ExportException e) {
            registry = LocateRegistry.getRegistry(port);
        } catch (RemoteException e) {
            return null;
        }

        registry.rebind("SapphireKernelServer", stub);
        spiedKs.setRegion(region);

        ServerInfo info = new ServerInfo(spiedKs.getLocalHost(), spiedKs.getRegion());
        spiedOms.registerKernelServer(info);
        return spiedKs;
    }
}
