package sapphire.common;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.addHost;
import static sapphire.common.SapphireUtils.dummyRegistry;
import static sapphire.common.SapphireUtils.getHostOnOmsKernelServerManager;
import static sapphire.common.SapphireUtils.startSpiedKernelServer;
import static sapphire.common.SapphireUtils.startSpiedOms;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import sapphire.app.SO;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.stubs.SO_Stub;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelObject;
import sapphire.kernel.server.KernelObjectManager;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.runtime.Sapphire;

/**
 * This is Base Test class for Multi-DM Unit testing, with a setup to create a spied instance of oms
 * and 3 kernel servers. This Class has necessary mocking needed for few methods in the code flow
 * for creating/deleting/replicating sapphire objects. TestCase files can inherit this class for
 * testing MultiDM scenarios.
 */
public class MultiPolicyChainBaseTest {
    protected DefaultSapphirePolicy.DefaultClientPolicy client;
    protected DefaultSapphirePolicy.DefaultServerPolicy server1;
    protected DefaultSapphirePolicy.DefaultServerPolicy server2;
    protected DefaultSapphirePolicy.DefaultServerPolicy server3;
    protected DefaultSapphirePolicy.DefaultGroupPolicy group;
    protected SO_Stub soStub;
    protected SO so;
    protected OMSServer spiedOms;
    protected KernelServer spiedKs1;
    protected KernelServer spiedKs2;
    protected KernelServer spiedKs3;

    protected boolean serversInSameRegion = true;

    public void setUpMultiDM(
            SapphireObjectSpec spec,
            HashMap<String, Class> groupMap,
            HashMap<String, Class> serverMap)
            throws Exception {
        PowerMockito.mockStatic(LocateRegistry.class);
        when(LocateRegistry.getRegistry(anyString(), anyInt())).thenReturn(dummyRegistry);

        // create a spied oms instance
        OMSServerImpl spiedOms = startSpiedOms();
        KernelServerImpl.oms = spiedOms;
        this.spiedOms = spiedOms;

        int kernelPort1 = 10001;
        int kernelPort2 = 10002;
        int kernelPort3 = 10003;

        String[] regions;
        if (serversInSameRegion) {
            regions = new String[] {"IND", "IND", "IND"};
        } else {
            regions = new String[] {"IND", "CHN", "USA"};
        }
        // create a spied kernel server instance
        spiedKs1 = startSpiedKernelServer(spiedOms, kernelPort1, regions[0]);
        spiedKs2 = startSpiedKernelServer(spiedOms, kernelPort2, regions[1]);
        spiedKs3 = startSpiedKernelServer(spiedOms, kernelPort3, regions[2]);

        // Set this instance of kernel server as local kernel server
        // Setting spiedKs3 as the local KernelServer, as KS3 maps to the first one with port 10001.
        GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs3;

        /* Add all the hosts to the kernel client of local kernel server instance so that every call
        becomes local invocation */
        addHost(spiedKs2);
        addHost(spiedKs1);

        // Stub static kernel object factory methods
        mockStatic(
                KernelObjectFactory.class,
                new Answer<Object>() {
                    int i = 0;

                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {

                        if (invocation.getMethod().getName().equals("createStub")) {
                            return invocation.callRealMethod();
                        }

                        KernelObjectStub stub = null;
                        KernelObjectStub spiedStub = null;
                        String policyObjectName = (String) invocation.getArguments()[0];
                        String temp[] = policyObjectName.split("\\$")[0].split("\\.");
                        String policyName = temp[temp.length - 1];
                        Class<?> groupClass = groupMap.get(policyName);
                        Class<?> serverClass = serverMap.get(policyName);

                        if (policyObjectName.contains("Server")) {
                            invocation.getArguments()[0] = serverClass.getName();
                            ++i;
                            stub = (KernelObjectStub) invocation.callRealMethod();
                            spiedStub = spy(stub);
                            DefaultSapphirePolicy.DefaultServerPolicy serverPolicyStub =
                                    (DefaultSapphirePolicy.DefaultServerPolicy) spiedStub;
                            if (1 == i) {
                                server1 = serverPolicyStub;
                            } else if (2 == i) {
                                server2 = serverPolicyStub;
                            } else if (3 == i) {
                                server3 = serverPolicyStub;
                            }

                        } else if (policyObjectName.contains("Group")) {
                            invocation.getArguments()[0] = groupClass.getName();
                            stub = (KernelObjectStub) invocation.callRealMethod();
                            group =
                                    (DefaultSapphirePolicy.DefaultGroupPolicy)
                                            (spiedStub = spy(stub));
                        }

                        KernelServer ks = null;
                        if (stub.$__getHostname()
                                .toString()
                                .contains(String.valueOf(kernelPort1))) {
                            ks = spiedKs1;
                        } else if (stub.$__getHostname()
                                .toString()
                                .contains(String.valueOf(kernelPort2))) {
                            ks = spiedKs2;
                        } else if (stub.$__getHostname()
                                .toString()
                                .contains(String.valueOf(kernelPort3))) {
                            ks = spiedKs3;
                        }

                        /* set this spied stub itself as kernel object so that we can verify
                        all the operations in test cases */
                        KernelObjectManager objMgr =
                                (KernelObjectManager)
                                        extractFieldValueOnInstance(ks, "objectManager");
                        objMgr.addObject(stub.$__getKernelOID(), new KernelObject(spiedStub));

                        return spiedStub;
                    }
                });

        // Stub static sapphire methods
        mockStatic(Sapphire.class);
        PowerMockito.mockStatic(
                Sapphire.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if ((invocation.getMethod().getName().equals("getAppStub"))) {
                            String appStubClassName = SO_Stub.class.getName();
                            Object[] arguments = invocation.getArguments();
                            SapphirePolicy.SapphireServerPolicy serverPolicy =
                                    (SapphirePolicy.SapphireServerPolicy)
                                            invocation.getArguments()[1];
                            Object[] args = (Object[]) invocation.getArguments()[2];

                            return Sapphire.extractAppStub(serverPolicy.$__initialize(spec, args));
                        }
                        if (!(invocation.getMethod().getName().equals("getPolicyStub")))
                            return invocation.callRealMethod();

                        if (invocation.getArguments().length == 2) {
                            KernelOID oid = (KernelOID) invocation.getArguments()[1];
                            KernelServer ks =
                                    getHostOnOmsKernelServerManager(
                                            spiedOms, spiedOms.lookupKernelObject(oid));
                            return ((KernelServerImpl) ks).getObject(oid);
                        }

                        return invocation.callRealMethod();
                    }
                });
    }

    public void tearDown() throws Exception {}
}
