package sapphire.common;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.SapphireUtils.addHost;
import static sapphire.common.SapphireUtils.dummyRegistry;
import static sapphire.common.SapphireUtils.getHostOnOmsKernelServerManager;
import static sapphire.common.SapphireUtils.startSpiedKernelServer;
import static sapphire.common.SapphireUtils.startSpiedOms;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.compiler.GlobalStubConstants.POLICY_STUB_PACKAGE;
import static sapphire.compiler.GlobalStubConstants.STUB_SUFFIX;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import sapphire.app.SapphireObjectServer;
import sapphire.app.SapphireObjectSpec;
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
import sapphire.sampleSO.stubs.SO_Stub;

/**
 * This is the base test class with a setup to create a spied instance of oms, 3 kernel servers. And
 * has the necessary mocking of few methods in creating/deleting/replicating sapphire object flow.
 * TestCase files can inherit this class and make their setup simple.
 */

/** Created by Venugopal Reddy K on 12/9/18. */
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class,
    Utils.ObjectCloner.class
})
public class BaseTest {
    protected DefaultSapphirePolicy.DefaultClientPolicy client;
    protected DefaultSapphirePolicy.DefaultServerPolicy server1;
    protected DefaultSapphirePolicy.DefaultServerPolicy server2;
    protected DefaultSapphirePolicy.DefaultServerPolicy server3;
    protected DefaultSapphirePolicy.DefaultGroupPolicy group;
    protected SO_Stub soStub; // client side stub
    protected OMSServer spiedOms;
    protected SapphireObjectServer sapphireObjServer;
    protected KernelServer spiedKs1;
    protected KernelServer spiedKs2;
    protected KernelServer spiedKs3;
    protected int kernelServerCount = 0;
    private int kernelPort1 = 10001;
    private int kernelPort2 = 10002;
    private int kernelPort3 = 10003;

    protected boolean serversInSameRegion = true;

    public void setUp(
            int serverCount,
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
        sapphireObjServer = spiedOms;

        String[] regions;
        if (serversInSameRegion) {
            regions = new String[] {"IND", "IND", "IND"};
        } else {
            regions = new String[] {"IND", "CHN", "USA"};
        }

        assert (serverCount <= 3);
        kernelServerCount = serverCount;

        /* create configured number of spied kernel server instances. And populate the servers map  in kernel clients of
        respective kernel servers so that each kernel server will be able to communicate with other servers.
        */
        spiedKs1 = startSpiedKernelServer(spiedOms, kernelPort1, regions[0]);

        if (serverCount > 1) {
            spiedKs2 = startSpiedKernelServer(spiedOms, kernelPort2, regions[1]);
            addHost(spiedKs1);
            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs1;
            addHost(spiedKs2);
        }
        if (serverCount > 2) {
            spiedKs3 = startSpiedKernelServer(spiedOms, kernelPort3, regions[2]);
            addHost(spiedKs1);
            addHost(spiedKs2);
            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs2;
            addHost(spiedKs3);
            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs1;
            addHost(spiedKs3);
        }

        PowerMockito.mockStatic(
                Utils.ObjectCloner.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (invocation.getMethod().getName().equals("deepCopy")
                                && !(invocation.getArguments()[0] instanceof AppObject)) {
                            return invocation.getArguments()[0];
                        }
                        return invocation.callRealMethod();
                    }
                });

        // Stub static kernel object factory methods
        mockStatic(
                KernelObjectFactory.class,
                new Answer<Object>() {
                    int i = 0;

                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (!invocation.getMethod().getName().equals("delete")
                                && !invocation.getMethod().getName().equals("create")) {
                            return invocation.callRealMethod();
                        }

                        if (invocation.getMethod().getName().equals("delete")) {
                            KernelOID oid = (KernelOID) invocation.getArguments()[0];
                            InetSocketAddress host = spiedOms.lookupKernelObject(oid);

                            KernelServer ks = null;
                            if (host.toString().contains(String.valueOf(kernelPort1))) {
                                ks = spiedKs1;
                            } else if (host.toString().contains(String.valueOf(kernelPort2))) {
                                ks = spiedKs2;
                            } else if (host.toString().contains(String.valueOf(kernelPort3))) {
                                ks = spiedKs3;
                            }

                            KernelServerImpl temp = GlobalKernelReferences.nodeServer;
                            GlobalKernelReferences.nodeServer = (KernelServerImpl) ks;
                            Object ret = invocation.callRealMethod();
                            GlobalKernelReferences.nodeServer = temp;
                            return ret;
                        }

                        assert (invocation.getMethod().getName().equals("create"));
                        String policyObjectName = (String) invocation.getArguments()[0];
                        String temp[] = policyObjectName.split("\\$")[0].split("\\.");
                        String policyName = temp[temp.length - 1];
                        Class<?> groupClass = groupMap.get(policyName);
                        Class<?> serverClass = serverMap.get(policyName);

                        if (policyObjectName.contains("Server")) {
                            String[] split = (serverClass.getName() + STUB_SUFFIX).split("\\.");
                            invocation.getArguments()[0] =
                                    POLICY_STUB_PACKAGE + "." + split[split.length - 1];
                        } else if (policyObjectName.contains("Group")) {
                            String[] split = (groupClass.getName() + STUB_SUFFIX).split("\\.");
                            invocation.getArguments()[0] =
                                    POLICY_STUB_PACKAGE + "." + split[split.length - 1];
                        }
                        return invocation.callRealMethod();
                    }
                });

        // Stub static sapphire methods
        mockStatic(Sapphire.class);
        PowerMockito.mockStatic(
                Sapphire.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
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

        SapphireObjectID sapphireObjId = sapphireObjServer.createSapphireObject(spec.toString());

        soStub = (SO_Stub) sapphireObjServer.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
        getServerAndGroupPolicyObjects();
    }

    private KernelServer getKernelServerFromPolicyStub(KernelObjectStub stub) {
        KernelServer ks = null;
        if (stub.$__getHostname().toString().contains(String.valueOf(kernelPort1))) {
            ks = spiedKs1;
        } else if (stub.$__getHostname().toString().contains(String.valueOf(kernelPort2))) {
            ks = spiedKs2;
        } else if (stub.$__getHostname().toString().contains(String.valueOf(kernelPort3))) {
            ks = spiedKs3;
        }

        return ks;
    }

    private void getServerPolicyObjects(KernelServer kernelServer, KernelObject obj) {
        KernelServerImpl ks = (KernelServerImpl) kernelServer;
        if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort1))) {
            server1 = (DefaultSapphirePolicy.DefaultServerPolicy) obj.getObject();
        } else if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort2))) {
            server2 = (DefaultSapphirePolicy.DefaultServerPolicy) obj.getObject();
        } else if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort3))) {
            server3 = (DefaultSapphirePolicy.DefaultServerPolicy) obj.getObject();
        }
    }

    private void getGroupPolicyObject(KernelServer kernelServer, KernelObject obj) {
        KernelServerImpl ks = (KernelServerImpl) kernelServer;
        if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort1))) {
            group = (DefaultSapphirePolicy.DefaultGroupPolicy) obj.getObject();
        } else if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort2))) {
            group = (DefaultSapphirePolicy.DefaultGroupPolicy) obj.getObject();
        } else if (ks.getLocalHost().toString().contains(String.valueOf(kernelPort3))) {
            group = (DefaultSapphirePolicy.DefaultGroupPolicy) obj.getObject();
        }
    }

    public void getServerAndGroupPolicyObjects() throws Exception {
        ArrayList<SapphirePolicy.SapphireServerPolicy> servers = client.getGroup().getServers();
        KernelServer ks = getKernelServerFromPolicyStub((KernelObjectStub) client.getGroup());
        KernelObjectManager objMgr =
                (KernelObjectManager) extractFieldValueOnInstance(ks, "objectManager");
        KernelObject obj =
                objMgr.lookupObject(((KernelObjectStub) client.getGroup()).$__getKernelOID());
        getGroupPolicyObject(ks, obj);
        for (SapphirePolicy.SapphireServerPolicy server : servers) {
            ks = getKernelServerFromPolicyStub((KernelObjectStub) server);
            objMgr = (KernelObjectManager) extractFieldValueOnInstance(ks, "objectManager");
            obj = objMgr.lookupObject(((KernelObjectStub) server).$__getKernelOID());
            getServerPolicyObjects(ks, obj);
        }
    }

    public void tearDown() throws Exception {}
}
