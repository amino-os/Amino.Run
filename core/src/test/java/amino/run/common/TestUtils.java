package amino.run.common;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.InstanceManager;
import amino.run.oms.KernelServerManager;
import amino.run.oms.MicroServiceManager;
import amino.run.oms.OMSServer;
import amino.run.oms.OMSServerImpl;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Created by Vishwajeet on 4/4/18. */
public class TestUtils {

    public static OMSServerImpl startSpiedOms(String ipAddr, int omsPort) throws Exception {
        mockStatic(
                UnicastRemoteObject.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (invocation.getMethod().getName().equals("exportObject")) {
                            /* return the object to be exported as it is. */
                            return invocation.getArguments()[0];
                        }
                        return invocation.callRealMethod();
                    }
                });

        OMSServerImpl.main(
                new String[] {
                    OMSServerImpl.OMS_IP_OPT, ipAddr, OMSServerImpl.OMS_PORT_OPT, "" + omsPort
                });

        /* Get the oms instance created above from the local kernel server's static oms field */
        OMSServerImpl myOms = (OMSServerImpl) KernelServerImpl.oms;

        /* If needed, all the fields inside OMSServer can be spied as shown below in commented code */
        /*KernelServerManager serverManager = (KernelServerManager)extractFieldValueOnInstance(myOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(myOms, "serverManager", spiedServerManager);*/

        OMSServerImpl spiedOms = spy(myOms);
        return spiedOms;
    }

    public static void startSpiedKernelServer(
            String ipAddr, int port, String omsIpaddr, int omsPort, String region)
            throws Exception {
        mockStatic(
                UnicastRemoteObject.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (invocation.getMethod().getName().equals("exportObject")) {
                            /* return the object to be exported as it is. */
                            return invocation.getArguments()[0];
                        }
                        return invocation.callRealMethod();
                    }
                });

        String labels = KernelServerImpl.REGION_KEY + "=" + region;
        KernelServerImpl.main(
                new String[] {
                    KernelServerImpl.KERNEL_SERVER_IP_OPT,
                    ipAddr,
                    KernelServerImpl.KERNEL_SERVER_PORT_OPT,
                    "" + port,
                    OMSServerImpl.OMS_IP_OPT,
                    omsIpaddr,
                    OMSServerImpl.OMS_PORT_OPT,
                    "" + omsPort,
                    KernelServerImpl.LABEL_OPT,
                    labels
                });

        /* Get the kernel server instance created above from the GlobalKernelReferences nodeServer field */
        KernelServerImpl ks = GlobalKernelReferences.nodeServer;

        /* Stop heartbeat between kernel server and oms */
        ResettableTimer heartbeatTimer =
                (ResettableTimer) extractFieldValueOnInstance(ks, "ksHeartbeatSendTimer");
        heartbeatTimer.cancel();

        KernelServerManager kernelServerManager =
                (KernelServerManager)
                        extractFieldValueOnInstance(KernelServerImpl.oms, "serverManager");
        Map<InetSocketAddress, Object> servers =
                (Map<InetSocketAddress, Object>)
                        extractFieldValueOnInstance(kernelServerManager, "servers");
        Object kernelServerInfo = servers.get(ks.getLocalHost());

        ResettableTimer heartBeatTimer =
                (ResettableTimer) extractFieldValueOnInstance(kernelServerInfo, "heartBeatTimer");
        heartBeatTimer.cancel();

        /* If needed, all the fields inside KernelServer can be spied as shown below in commented code */
        /*KernelClient kernelClient = (KernelClient) extractFieldValueOnInstance(ks, "client");
        KernelClient spiedKernelClient = spy(kernelClient);
        setFieldValueOnInstance(ks, "client", spiedKernelClient);*/
    }

    public static InstanceManager getOmsMicroService(OMSServer oms, MicroServiceID microServiceId)
            throws Exception {
        MicroServiceManager mgr =
                (MicroServiceManager) extractFieldValueOnInstance(oms, "objectManager");
        ConcurrentHashMap<MicroServiceID, InstanceManager> microServices =
                (ConcurrentHashMap<MicroServiceID, InstanceManager>)
                        extractFieldValueOnInstance(mgr, "microServices");
        return microServices.get(microServiceId);
    }
}
