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

        KernelServerManager serverManager =
                (KernelServerManager) extractFieldValueOnInstance(spiedOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(spiedOms, "serverManager", spiedServerManager);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 10003);
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
        ServerInfo srvinfo = new ServerInfo(new InetSocketAddress("127.0.0.1", 10001), "IND");
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

    @Test
    public void removeObjectTest() throws Exception {
        KernelServerImpl ks = (KernelServerImpl) spiedKs3;
        ks.deleteKernelObject(client.getServer().$__getKernelOID());
        try {
            KernelObject object = ks.getKernelObject(client.getServer().$__getKernelOID());
        } catch (Exception e) {

            assertEquals(
                    e.getClass().getName(), "sapphire.kernel.common.KernelObjectNotFoundException");
        }
    }

    @Test
    public void testMain() throws Exception {
        PowerMockito.mockStatic(LocateRegistry.class);
        when(LocateRegistry.getRegistry(anyString(), anyInt())).thenCallRealMethod();
        when(LocateRegistry.createRegistry(anyInt())).thenCallRealMethod();
        OMSServerImpl.main(new String[] {"127.0.0.1", "10006"});
        KernelServerImpl.main(
                new String[] {
                    "127.0.0.1",
                    "10001",
                    "127.0.0.1",
                    "10006",
                    "IND",
                    "--labels=label",
                    "--servicePort=10010"
                });
        // for testing negative scenario with only 3 arguments
        KernelServerImpl.main(new String[] {"127.0.0.1", "10001", "127.0.0.1"});
    }

    @Test
    public void getAllKernelObjectOidsTest() throws Exception {
        KernelObjectManager kom =
                (KernelObjectManager) extractFieldValueOnInstance(spiedKs3, "objectManager");
        assertEquals(2, kom.getAllKernelObjectOids().length);
    }
}
