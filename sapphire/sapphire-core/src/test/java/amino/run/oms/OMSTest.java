package amino.run.oms;

import static org.junit.Assert.assertEquals;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.BaseTest;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireUtils;
import amino.run.sampleSO.SO;
import amino.run.sampleSO.stubs.SO_Stub;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** OMS API test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
public class OMSTest extends BaseTest {
    SO so;
    @Rule public ExpectedException thrown;

    public OMSTest() {
        thrown = ExpectedException.none();
    }

    @Before
    public void setUp() throws Exception {
        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .create();
        super.setUp(1, spec);
        so = ((SO) (server1.sapphire_getAppObject().getObject()));
    }

    @Test
    public void acquireSapphireObjectStubSuccessTest() throws Exception {
        SapphireObjectID sid = group.getSapphireObjId();
        SO_Stub appObjstub = (SO_Stub) sapphireObjServer.acquireSapphireObjectStub(sid);
        assertEquals(
                1,
                SapphireUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());

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
                2,
                SapphireUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(100));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(100), so.getI());

        /* Reference count must become 1(decrement by 1) upon detach */
        sapphireObjServer.detachFromSapphireObject("MySapphireObject");
        assertEquals(
                1,
                SapphireUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());
    }

    @Test
    public void getServerTest() throws Exception {
        List<InetSocketAddress> servers = spiedOms.getServers(null);

        /* Reference count must become 1 (Since 1 kernel server is added )  */
        assertEquals(new Integer(kernelServerCount), new Integer(servers.size()));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
