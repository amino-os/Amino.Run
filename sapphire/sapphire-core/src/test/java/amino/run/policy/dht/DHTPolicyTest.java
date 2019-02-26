package amino.run.policy.dht;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.BaseTest;
import amino.run.sampleSO.SO;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DHTPolicyTest extends BaseTest {
    @Before
    public void setup() throws Exception {
        DHTPolicy.Config config = new DHTPolicy.Config();
        config.setNumOfShards(3);

        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(DHTPolicy.class.getName())
                                        .addConfig(config)
                                        .create())
                        .create();
        serversInSameRegion = false;
        super.setUp(3, spec);
    }

    @Test
    public void test() throws Exception {
        String method = "public void amino.run.sampleSO.SO.incI(java.lang.Integer)";
        int loopCount = 10;
        for (int i = 0; i < loopCount; i++) {
            /* Increment value of I each time by 1,2,3,4 .. so on till loopCount. Each request would go to any one of
            the 3 available shards */
            ArrayList<Object> params = new ArrayList<>(Arrays.asList(new Object[] {i + 1}));
            client.onRPC(method, params);
        }

        /* Verify that all requests have been processed. Aggregate of I value in all the shards must be equal
        to (loopCount * (loopCount + 1))/2 */
        int cnt = 0;
        cnt += ((SO) server1.getAppObject().getObject()).getI();
        cnt += ((SO) server2.getAppObject().getObject()).getI();
        cnt += ((SO) server3.getAppObject().getObject()).getI();

        Assert.assertEquals((loopCount * (loopCount + 1)) / 2, cnt);
    }
}
