package sapphire.kernel.server;

import static org.powermock.api.mockito.PowerMockito.mock;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.BaseTest;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelRPC;
import sapphire.kernel.common.KernelRPCException;
import sapphire.kernel.common.ServerInfo;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.util.ResettableTimer;
import sapphire.runtime.Sapphire;
import sapphire.sampleSO.SO;

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
    SO so;
    @Rule public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.sampleSO.SO")
                        .create();
        super.setUp(
                1,
                spec,
                new HashMap<String, Class>() {
                    {
                        put(
                                "DefaultSapphirePolicy",
                                DefaultSapphirePolicy.DefaultGroupPolicy.class);
                    }
                },
                new HashMap<String, Class>() {
                    {
                        put(
                                "DefaultSapphirePolicy",
                                DefaultSapphirePolicy.DefaultServerPolicy.class);
                    }
                });
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
        spiedKs1.makeKernelRPC(rpc);
    }
}
