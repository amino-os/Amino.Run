package amino.run.policy.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import amino.run.common.AppObject;
import org.junit.Test;

public class AppObjectShimServerPolicyTest {
    @Test
    public void test_policy_saves_copy() throws Exception {
        Integer[] originCore = new Integer[] {1, 2, 3};
        AppObject origin = new AppObject(originCore);

        AppObjectShimServerPolicy policy =
                AppObjectShimServerPolicy.cloneInShimServerPolicy(origin);
        AppObject retrieved = policy.getAppObject();

        assertNotSame("AppObject retrieved is a brand new one", origin, retrieved);
        assertNotSame(
                "Core of AppObject retrieved is a deep copy",
                origin.getObject(),
                retrieved.getObject());
        Integer[] retrievedCore = (Integer[]) retrieved.getObject();
        retrievedCore[0] = 777;
        assertEquals("original value should not affected", 1, (long) originCore[0]);
    }
}
