package amino.run.policy.scalability;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static org.junit.Assert.assertEquals;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.BaseTest;
import amino.run.policy.SapphirePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** ScaleupFrontend DM test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
public class ScaleUpFrontendPolicyTest extends BaseTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {

        ScaleUpFrontendPolicy.Config scaleConfig = new ScaleUpFrontendPolicy.Config();
        scaleConfig.setReplicationRateInMs(400);

        LoadBalancedFrontendPolicy.Config lbConfig = new LoadBalancedFrontendPolicy.Config();
        lbConfig.setMaxConcurrentReq(2);
        lbConfig.setReplicaCount(2);

        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(ScaleUpFrontendPolicy.class.getName())
                                        .addConfig(scaleConfig)
                                        .addConfig(lbConfig)
                                        .create())
                        .create();
        super.setUp(3, spec);
    }

    @Test
    public void clientTest() throws Exception {
        String methodName = "public java.lang.Integer amino.run.sampleSO.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();

        AtomicInteger syncCtrCurr =
                (AtomicInteger) extractFieldValueOnInstance(this.client, "replicaListSyncCtr");
        assertEquals(0, syncCtrCurr.get());
        assertEquals(null, extractFieldValueOnInstance(this.client, "replicaList"));

        this.client.onRPC(methodName, params);
        syncCtrCurr =
                (AtomicInteger) extractFieldValueOnInstance(this.client, "replicaListSyncCtr");
        assertEquals(1, syncCtrCurr.get());

        ArrayList<SapphirePolicy.SapphireServerPolicy> replicas =
                (ArrayList<SapphirePolicy.SapphireServerPolicy>)
                        extractFieldValueOnInstance(this.client, "replicaList");
        ArrayList<SapphirePolicy.SapphireServerPolicy> expected = this.group.getServers();
        Assert.assertArrayEquals(replicas.toArray(), expected.toArray());
    }

    @Test
    public void serverScaleUpTest() throws Exception {
        final String methodName = "public java.lang.Integer amino.run.sampleSO.SO.getIDelayed()";
        final ArrayList<Object> params = new ArrayList<Object>();
        Integer max = (Integer) extractFieldValueOnInstance(this.server1, "maxConcurrentReq");

        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();
        for (int i = 0; i < 2 * max + 1; i++) {
            FutureTask<Object> task =
                    new FutureTask<Object>(
                            new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    Object test = new String("test");
                                    try {
                                        test = client.onRPC(methodName, params);
                                    } catch (ServerOverLoadException e) {
                                    }
                                    return test;
                                }
                            });
            taskList.add(task);
        }

        // Run tasks in parallel
        ExecutorService executor = Executors.newFixedThreadPool(taskList.size());
        for (FutureTask<Object> t : taskList) {
            executor.execute(t);
        }

        for (int i = 0; i < taskList.size(); i++) {
            Object ret = taskList.get(i).get();
        }
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
