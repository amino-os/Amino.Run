package amino.run.kernel.server;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static amino.run.common.UtilsTest.setFieldValueOnInstance;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.BaseTest;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelRPC;
import amino.run.kernel.common.KernelRPCException;
import amino.run.oms.KernelServerManager;
import amino.run.sampleSO.SO;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** Created by Vishwajeet on 11/9/18. */
@RunWith(PowerMockRunner.class)
public class KSTest extends BaseTest {
    SO so;
    @Rule public ExpectedException thrown = ExpectedException.none();
    // Added to allow SystemExit in order to prevent termination of code
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Before
    public void setUp() throws Exception {
        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .create();
        super.setUp(1, spec);
        // Mocked getBestSuitableServer in KernelServerManager to always return spiedKs1 as the
        // bestsuitableServer in order to get exact result from
        // KernelObjectManager.getAllKernelObjectOids. If not mocked, random
        // server will be choosen each time making the results random.
        KernelServerManager serverManager =
                (KernelServerManager) extractFieldValueOnInstance(spiedOms, "serverManager");
        KernelServerManager spiedServerManager = spy(serverManager);
        setFieldValueOnInstance(spiedOms, "serverManager", spiedServerManager);
        InetSocketAddress address = new InetSocketAddress(LOOP_BACK_IP_ADDR, kernelPort1);
        doReturn(address)
                .when(spiedServerManager)
                .getBestSuitableServer(any(SapphireObjectSpec.class));
        so = ((SO) (server1.sapphire_getAppObject().getObject()));
    }

    @Test
    public void testMakeKernelRPC() throws Exception {
        String method = "public java.lang.Integer amino.run.sampleSO.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        KernelRPC rpc = new KernelRPC(server1.$__getKernelOID(), method, params);
        thrown.expect(KernelRPCException.class);
        // Modified the KernelRPC call to ks1 as the local
        // KernelServer has been changed to KS1, as part of Multi-DM implementation.
        spiedKs1.makeKernelRPC(rpc);
    }

    @Test(expected = KernelObjectNotFoundException.class)
    public void removeObjectTest() throws Exception {
        /* used spiedKs1 here because in setUp(), getBestSuitableServer is mocked in such a way that
        it should always return spiedKs1. If other KernelServers are used,
        deleteKernelObject will throw the exception */
        KernelServerImpl ks = (KernelServerImpl) spiedKs1;
        ks.deleteKernelObject(client.getServer().$__getKernelOID());
        // should throw KernelObjectNotFoundException as it is deleted
        ks.getKernelObject(client.getServer().$__getKernelOID());
    }

    @Test
    public void testMain() throws Exception {
        // for testing negative scenario with only 3 arguments
        KernelServerImpl.main(
                new String[] {LOOP_BACK_IP_ADDR, Integer.toString(kernelPort1), LOOP_BACK_IP_ADDR});
    }

    @Test
    public void getAllKernelObjectOidsTest() throws Exception {
        // used spiedKs1 here because in setUp(), getBestSuitableServer is mocked in such a way that
        // it should always return spiedKs1. If other KernelServers are used,
        // KernelObjectManager.getAllKernelObjectOids() will return null.
        KernelObjectManager kom =
                (KernelObjectManager) extractFieldValueOnInstance(spiedKs1, "objectManager");
        // As KernelObjectManager.addObject is called once during
        // createSapphireObject, length returned should be 1.
        assertEquals(new Integer(1), new Integer(kom.getAllKernelObjectOids().length));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
