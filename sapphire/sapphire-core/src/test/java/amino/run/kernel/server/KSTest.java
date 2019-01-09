package amino.run.kernel.server;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.BaseTest;
import amino.run.kernel.common.KernelRPC;
import amino.run.kernel.common.KernelRPCException;
import amino.run.sampleSO.SO;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** Created by Vishwajeet on 11/9/18. */
@RunWith(PowerMockRunner.class)
public class KSTest extends BaseTest {
    SO so;
    @Rule public ExpectedException thrown = ExpectedException.none();

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
    public void testMakeKernelRPC() throws Exception {
        String method = "public java.lang.Integer amino.run.sampleSO.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        KernelRPC rpc = new KernelRPC(server1.$__getKernelOID(), method, params);
        thrown.expect(KernelRPCException.class);
        // Modified the KernelRPC call from spiedKs1 to spiedKs3 as the local
        // KernelServer has been changed to KS3, as part of Multi-DM implementation.
        spiedKs1.makeKernelRPC(rpc);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
