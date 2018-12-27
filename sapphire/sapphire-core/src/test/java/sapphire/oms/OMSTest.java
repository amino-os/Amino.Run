package sapphire.oms;

import static org.junit.Assert.assertEquals;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.SapphireUtils.getOmsSapphireInstance;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.List;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.BaseTest;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.runtime.Sapphire;
import sapphire.sampleSO.SO;
import sapphire.sampleSO.stubs.SO_Stub;

/** OMS API test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class OMSTest extends BaseTest {
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
    public void acquireSapphireObjectStubSuccessTest() throws Exception {
        SapphireObjectID sid = group.getSapphireObjId();
        SO_Stub appObjstub = (SO_Stub) sapphireObjServer.acquireSapphireObjectStub(sid);
        assertEquals(
                1, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(10));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(10), so.getI());
    }

    @Test
    public void attachAndDetactSapphireObjectSuccessTest() throws Exception {
        sapphireObjServer.setSapphireObjectName(group.getSapphireObjId(), "MySapphireObject");

        SO_Stub appObjstub = (SO_Stub) sapphireObjServer.attachToSapphireObject("MySapphireObject");

        /* Reference count must become 2. Once user created it and other attached to it */
        assertEquals(
                2, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(100));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(100), so.getI());

        /* Reference count must become 1(decrement by 1) upon detach */
        sapphireObjServer.detachFromSapphireObject("MySapphireObject");
        assertEquals(
                1, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());
    }

    @Test
    public void getServerTest() throws Exception {
        List<InetSocketAddress> servers = spiedOms.getServers(null);

        /* Reference count must become 1 (Since 1 kernel server is added )  */
        assertEquals(new Integer(kernelServerCount), new Integer(servers.size()));
    }

    @Test
    public void mainTest() throws Exception {

        OMSServerImpl.main(new String[] {"127.0.0.1", "10005"});
    }

    @After
    public void tearDown() throws Exception {
        deleteSapphireObject(spiedOms, group.getSapphireObjId());
    }
}
