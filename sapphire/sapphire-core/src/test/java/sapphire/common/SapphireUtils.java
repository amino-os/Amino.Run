package sapphire.common;

import static org.mockito.Mockito.spy;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServerImpl;

/** Created by Vishwajeet on 4/4/18. */
public class SapphireUtils {
    public static int omsPort = 10000;
    public static String LOOP_BACK_IP_ADDR = "127.0.0.1";

    public static OMSServerImpl startSpiedOms(String appEntryClassName) throws Exception {
        OMSServerImpl myOms = new OMSServerImpl();

        /* If needed, all the fields inside OMSServer can be spied as shown below in commented code */
        /*KernelServerManager serverManager = (KernelServerManager)extractFieldValueOnInstance(myOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(myOms, "serverManager", spiedServerManager);*/

        OMSServerImpl spiedOms = spy(myOms);

        return spiedOms;
    }

    public static KernelServerImpl startSpiedKernelServer(
            OMSServerImpl spiedOms, int port, String region) throws Exception {
        KernelServerImpl ks =
                new KernelServerImpl(
                        new InetSocketAddress(LOOP_BACK_IP_ADDR, port),
                        new InetSocketAddress(LOOP_BACK_IP_ADDR, omsPort));
        ks.setRegion(region);
        KernelServerImpl.oms = spiedOms;

        /* If needed, all the fields inside KernelServer can be spied as shown below in commented code */
        /*KernelClient kernelClient = (KernelClient) extractFieldValueOnInstance(ks, "client");
        KernelClient spiedKernelClient = spy(kernelClient);
        setFieldValueOnInstance(ks, "client", spiedKernelClient);*/

        KernelServerImpl spiedKs = spy(ks);
        spiedOms.registerKernelServer(new ServerInfo(spiedKs.getLocalHost(), spiedKs.getRegion()));
        return spiedKs;
    }

    public static void addHost(KernelServer ks) throws Exception {
        Hashtable<InetSocketAddress, KernelServer> servers =
                (Hashtable<InetSocketAddress, KernelServer>)
                        extractFieldValueOnInstance(
                                GlobalKernelReferences.nodeServer.getKernelClient(), "servers");
        servers.put(((KernelServerImpl) ks).getLocalHost(), ks);
    }
}
