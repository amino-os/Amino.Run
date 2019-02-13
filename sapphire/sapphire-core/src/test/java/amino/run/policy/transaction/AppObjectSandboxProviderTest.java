package amino.run.policy.transaction;

import static org.junit.Assert.assertNotSame;

import amino.run.common.AppObject;
import java.io.Serializable;
import java.util.UUID;
import org.junit.Test;

public class AppObjectSandboxProviderTest {
    @Test
    public void test_get_copy_of_appObject() throws Exception {
        UUID txId = UUID.randomUUID();
        Serializable coreOrigin = "aaa";
        AppObject originAppObject = new AppObject(coreOrigin);
        TwoPCCohortPolicy.ServerPolicy origin = new TwoPCCohortPolicy.ServerPolicy();
        origin.$__initialize(originAppObject);

        AppObjectSandboxProvider provider = new AppObjectSandboxProvider();
        AppObjectShimServerPolicy sandbox =
                (AppObjectShimServerPolicy) provider.getSandbox(origin, txId);

        AppObject sandboxAppObject = sandbox.getAppObject();
        assertNotSame(sandboxAppObject, originAppObject);
    }
}
