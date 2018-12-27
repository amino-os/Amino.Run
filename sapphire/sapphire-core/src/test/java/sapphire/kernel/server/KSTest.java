package sapphire.kernel.server;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;
import static sapphire.common.UtilsTest.setFieldValueOnInstance;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.Language;
import sapphire.app.SapphireObject;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.BaseTest;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.kernel.common.ServerInfo;
import sapphire.oms.KernelServerManager;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.ResettableTimer;
import sapphire.runtime.Sapphire;
import sapphire.sampleSO.SO;
import sapphire.sampleSO.stubs.SO_Stub;

/** Created by Vishwajeet on 11/9/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class KSTest extends BaseTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    // Added to allow SystemExit in order to prevent termination of code
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    public static class DefaultSO extends SO implements SapphireObject {}

    public static class Group_Stub extends DefaultSapphirePolicy.DefaultGroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public Group_Stub(sapphire.kernel.common.KernelOID oid) {
            this.$__oid = oid;
        }

        public sapphire.kernel.common.KernelOID $__getKernelOID() {
            return this.$__oid;
        }

        public java.net.InetSocketAddress $__getHostname() {
            return this.$__hostname;
        }

        public void $__updateHostname(java.net.InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    public static class Server_Stub extends DefaultSapphirePolicy.DefaultServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public Server_Stub(KernelOID oid) {
            this.oid = oid;
            this.$__oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return $__oid;
        }

        public InetSocketAddress $__getHostname() {
            return $__hostname;
        }

        public void $__updateHostname(InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }
    }

    @Before
    public void setUp() throws Exception {
        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.sampleSO.SO")
                        .create();
        super.setUp(
                spec,
                new HashMap<String, Class>() {
                    {
                        put("DefaultSapphirePolicy", Group_Stub.class);
                    }
                },
                new HashMap<String, Class>() {
                    {
                        put("DefaultSapphirePolicy", Server_Stub.class);
                    }
                });

        // Mocked getBestSuitableServer in KernelServerManager to always return spiedKs3 as the
        // bestsuitableServer in order to get exact result from
        // KernelObjectManager.getAllKernelObjectOids. If not mocked, random
        // server will be choosen each time making the results random.
        KernelServerManager serverManager =
                (KernelServerManager) extractFieldValueOnInstance(spiedOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(spiedOms, "serverManager", spiedServerManager);
        InetSocketAddress address = new InetSocketAddress(localIP, kernelPort3);
        doReturn(address)
                .when(spiedServerManager)
                .getBestSuitableServer(any(SapphireObjectSpec.class));

        SapphireObjectID sapphireObjId = sapphireObjServer.createSapphireObject(spec.toString());

        soStub = (SO_Stub) sapphireObjServer.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
        so = ((SO) (server1.sapphire_getAppObject().getObject()));
    }

    @Test
    public void testHeartbeat() throws Exception {
        Field field = PowerMockito.field(KernelServerImpl.class, "ksHeartbeatSendTimer");
        field.set(KernelServerImpl.class, mock(ResettableTimer.class));
        ServerInfo srvinfo = new ServerInfo(new InetSocketAddress(localIP, kernelPort1), "IND");
        KernelServerImpl.startheartbeat(srvinfo);
    }

    @Test
    public void testMakeKernelRPC() throws Exception {
        String method = "public java.lang.Integer sapphire.sampleSO.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        KernelRPC rpc = new KernelRPC(server1.$__getKernelOID(), method, params);
        thrown.expect(KernelRPCException.class);
        // Modified the KernelRPC call from spiedKs1 to spiedKs3 as the local
        // KernelServer has been changed to KS3, as part of Multi-DM implementation.
        spiedKs3.makeKernelRPC(rpc);
    }

    @Test(expected = KernelObjectNotFoundException.class)
    public void removeObjectTest() throws Exception {
        KernelServerImpl ks = (KernelServerImpl) spiedKs3;
        /* used spiedKs3 here because in setUp(), getBestSuitableServer is mocked in such a way that
        it should always return spiedKs3. If other KernelServers are used,
        deleteKernelObject will throw the exception */
        ks.deleteKernelObject(client.getServer().$__getKernelOID());
        // should throw KernelObjectNotFoundException as it is deleted
        ks.getKernelObject(client.getServer().$__getKernelOID());
    }

    @Test
    public void testMain() throws Exception {
        PowerMockito.mockStatic(LocateRegistry.class);
        when(LocateRegistry.getRegistry(anyString(), anyInt())).thenCallRealMethod();
        when(LocateRegistry.createRegistry(anyInt())).thenCallRealMethod();
        OMSServerImpl.main(
                new String[] {localIP, Integer.toString(omsPort), "--servicePort=10010"});
        KernelServerImpl.main(
                new String[] {
                    localIP,
                    Integer.toString(kernelPort1),
                    localIP,
                    Integer.toString(omsPort),
                    "IND",
                    "--labels=label",
                    "--servicePort=10010"
                });
        // for testing negative scenario with only 3 arguments
        KernelServerImpl.main(new String[] {localIP, Integer.toString(kernelPort1), localIP});
    }

    @Test
    public void getAllKernelObjectOidsTest() throws Exception {
        // used spiedKs3 here because in setUp(), getBestSuitableServer is mocked in such a way that
        // it should always return spiedKs3. If other KernelServers are used,
        // KernelObjectManager.getAllKernelObjectOids() will return null.
        KernelObjectManager kom =
                (KernelObjectManager) extractFieldValueOnInstance(spiedKs3, "objectManager");
        // As KernelObjectManager.addObject is called twice, One during
        // createSapphireObject and the other in BaseTest.java, length returned should be 2.
        assertEquals(new Integer(2), new Integer(kom.getAllKernelObjectOids().length));
    }
}
