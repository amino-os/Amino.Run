package amino.run.common;

import static amino.run.common.SapphireUtils.addHost;
import static amino.run.common.SapphireUtils.startSpiedKernelServer;
import static amino.run.common.SapphireUtils.startSpiedOms;
import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import amino.run.app.SapphireObjectServer;
import amino.run.app.SapphireObjectSpec;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectFactory;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.server.KernelObject;
import amino.run.kernel.server.KernelObjectManager;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.oms.OMSServerImpl;
import amino.run.policy.DefaultSapphirePolicy;
import amino.run.policy.SapphirePolicy;
import amino.run.runtime.Sapphire;
import amino.run.sampleSO.stubs.SO_Stub;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

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
    OMSServerImpl.class,
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
    protected KernelServer spiedksOnOms;
    protected KernelServer spiedKs1;
    protected KernelServer spiedKs2;
    protected KernelServer spiedKs3;
    protected int kernelServerCount = 0;
    private String LOOP_BACK_IP_ADDR = "127.0.0.1";
    private int omsPort = 10000;
    private int kernelPort1 = 10001;
    private int kernelPort2 = 10002;
    private int kernelPort3 = 10003;

    protected boolean serversInSameRegion = true;

    public void setUp(int serverCount, SapphireObjectSpec spec) throws Exception {
        // create a spied oms instance
        OMSServerImpl spiedOms = startSpiedOms(LOOP_BACK_IP_ADDR, omsPort);
        spiedksOnOms = spy(GlobalKernelReferences.nodeServer);
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

        /* create configured number of spied kernel server instances. And populate the servers map in kernel clients of
        respective kernel servers so that each kernel server will be able to communicate with other servers without
        registry.lookup.
        */
        spiedKs1 =
                startSpiedKernelServer(
                        LOOP_BACK_IP_ADDR, kernelPort1, LOOP_BACK_IP_ADDR, omsPort, regions[0]);
        addHost(spiedksOnOms);
        if (serverCount > 1) {
            spiedKs2 =
                    startSpiedKernelServer(
                            LOOP_BACK_IP_ADDR, kernelPort2, LOOP_BACK_IP_ADDR, omsPort, regions[1]);
            addHost(spiedksOnOms);
            addHost(spiedKs1);
            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs1;
            addHost(spiedKs2);
        }
        if (serverCount > 2) {
            spiedKs3 =
                    startSpiedKernelServer(
                            LOOP_BACK_IP_ADDR, kernelPort3, LOOP_BACK_IP_ADDR, omsPort, regions[2]);
            addHost(spiedksOnOms);
            addHost(spiedKs1);
            addHost(spiedKs2);
            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs2;
            addHost(spiedKs3);
            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedKs1;
            addHost(spiedKs3);
        }

        // Stub static kernel object factory methods
        mockStatic(
                KernelObjectFactory.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if (!invocation.getMethod().getName().equals("delete")) {
                            return invocation.callRealMethod();
                        }

                        KernelOID oid = (KernelOID) invocation.getArguments()[0];
                        InetSocketAddress host = spiedOms.lookupKernelObject(oid);

                        KernelServer ks = null;
                        if (host.toString().contains(String.valueOf(omsPort))) {
                            ks = spiedksOnOms;
                        } else if (host.toString().contains(String.valueOf(kernelPort1))) {
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
                });

        // Stub static sapphire methods
        mockStatic(
                Sapphire.class,
                new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        if ((invocation.getMethod().getName().equals("createGroupPolicy"))) {
                            /* Group policy objects are created on kernel server within OMS */
                            KernelServerImpl temp = GlobalKernelReferences.nodeServer;
                            GlobalKernelReferences.nodeServer = (KernelServerImpl) spiedksOnOms;
                            Object ret = invocation.callRealMethod();
                            GlobalKernelReferences.nodeServer = temp;
                            return ret;
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

    private void getServerAndGroupPolicyObjects() throws Exception {
        ArrayList<SapphirePolicy.SapphireServerPolicy> servers = client.getGroup().getServers();
        KernelServer ks = spiedksOnOms;
        KernelObjectManager objMgr =
                (KernelObjectManager) extractFieldValueOnInstance(ks, "objectManager");
        KernelObject obj =
                objMgr.lookupObject(((KernelObjectStub) client.getGroup()).$__getKernelOID());
        group = (DefaultSapphirePolicy.DefaultGroupPolicy) obj.getObject();
        for (SapphirePolicy.SapphireServerPolicy server : servers) {
            ks = getKernelServerFromPolicyStub((KernelObjectStub) server);
            objMgr = (KernelObjectManager) extractFieldValueOnInstance(ks, "objectManager");
            obj = objMgr.lookupObject(((KernelObjectStub) server).$__getKernelOID());
            getServerPolicyObjects(ks, obj);
        }
    }

    public void tearDown() throws Exception {
        sapphireObjServer.deleteSapphireObject(group.getSapphireObjId());
    }
}
