package amino.run.kernel.server;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static junit.framework.TestCase.assertEquals;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.BaseTest;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelRPC;
import amino.run.kernel.common.KernelRPCException;
import amino.run.sampleSO.SO;
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
public class KernelServerTest extends BaseTest {
    SO so;
    @Rule public ExpectedException thrown = ExpectedException.none();
    // Added to allow SystemExit in order to prevent termination of code
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Before
    public void setUp() throws Exception {
        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .create();
        super.setUp(1, spec);
        so = ((SO) (server1.sapphire_getAppObject().getObject()));
    }

    @Test
    public void testMakeKernelRPC() throws Exception {
        String method = "public java.lang.Integer amino.run.sampleSO.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        /* Make kernel rpc with a method name not present in server policy so that make rpc fails at invocation time(but
        succeeds at object lookup).
        Note: We have passed app method name instead of server's onRPC method to fail the rpc invocation. */
        KernelRPC rpc = new KernelRPC(server1.$__getKernelOID(), method, params);
        thrown.expect(KernelRPCException.class);
        spiedKs1.makeKernelRPC(rpc);
    }

    @Test
    public void getKernelObjectTest() throws Exception {
        KernelServerImpl ks = (KernelServerImpl) spiedKs1;

        // Get the existing kernel object
        ks.getKernelObject(client.getServer().$__getKernelOID());
    }

    @Test(expected = KernelObjectNotFoundException.class)
    public void getNonExistentKernelObjectTest() throws Exception {
        KernelServerImpl ks = (KernelServerImpl) spiedKs1;

        // Get the non-existent kernel object. It must throw kernel object not found exception.
        ks.getKernelObject(new KernelOID(0));
    }

    @Test
    public void testMain() throws Exception {
        // for testing negative scenario with only 3 arguments
        KernelServerImpl.main(
                new String[] {LOOP_BACK_IP_ADDR, Integer.toString(kernelPort1), LOOP_BACK_IP_ADDR});
    }

    @Test
    public void getAllKernelObjectOidsTest() throws Exception {
        KernelObjectManager kom =
                (KernelObjectManager) extractFieldValueOnInstance(spiedKs1, "objectManager");
        // As KernelObjectManager.addObject is called once during
        // create, length returned should be 1.
        assertEquals(new Integer(1), new Integer(kom.getAllKernelObjectOids().length));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
