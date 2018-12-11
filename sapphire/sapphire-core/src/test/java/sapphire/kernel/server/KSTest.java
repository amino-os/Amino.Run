package sapphire.kernel.server;

import static org.powermock.api.mockito.PowerMockito.mock;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.Language;
import sapphire.app.SO;
import sapphire.app.SapphireObject;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.stubs.SO_Stub;
import sapphire.common.BaseTest;
import sapphire.common.LabelUtils;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.kernel.common.ServerInfo;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.util.ResettableTimer;
import sapphire.runtime.Sapphire;

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
                        .setJavaClassName("sapphire.app.SO")
                        .create();
        super.setUp(spec, Server_Stub.class, Group_Stub.class);

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
        String Labelstr = LabelUtils.LABEL_OPT + LabelUtils.REGION_KEY + "=IND";
        ServerInfo srvinfo =
                KernelServerImpl.createServerInfo(
                        new InetSocketAddress("127.0.0.1", 10001), Labelstr);
        KernelServerImpl.startheartbeat(srvinfo);
    }

    @Test
    public void testMakeKernelRPC() throws Exception {
        String method = "public java.lang.Integer sapphire.app.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        KernelRPC rpc = new KernelRPC(server1.$__getKernelOID(), method, params);
        thrown.expect(KernelRPCException.class);
        // Modified the KernelRPC call from spiedKs1 to spiedKs3 as the local
        // KernelServer has been changed to KS3, as part of Multi-DM implementation.
        spiedKs3.makeKernelRPC(rpc);
    }
}
