package amino.run.app;

import amino.run.common.BaseTest;
import amino.run.policy.dht.DHTPolicy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class NodeSelectorSpecTest extends BaseTest {
    DHTPolicy.Config config;

    @Before
    public void setup() throws Exception {
        config = new DHTPolicy.Config();
        config.setNumOfShards(3);
    }

    // Configure MicroService spec to deploy on same region
    @Test
    public void testOnlyOneRegionNodeSelection() throws Exception {
        //        MicroServiceSpec spec =
        //                MicroServiceSpec.newBuilder()
        //                        .setLang(Language.java)
        //                        .setJavaClassName("amino.run.sampleSO.SO")
        //                        .addDMSpec(
        //                                DMSpec.newBuilder()
        //                                        .setName(DHTPolicy.class.getName())
        //                                        .addConfig(config)
        //                                        .create())
        //                        .setNodeSelectorSpec(
        //                                new NodeSelectorSpec()
        //                                        .addRequireExpressions( // added expression for
        // use IND
        //                                                // region
        //                                                new NodeSelectorTerm()
        //                                                        .add(
        //                                                                new Requirement(
        //                                                                        REGION_KEY,
        //                                                                        Operator.Equal,
        //
        // Collections.singletonList(
        //                                                                                "IND")))))
        //                        .create();
        //        serversInSameRegion = false;
        //        // setup 3 kernel server with IND, CHN and USA as region respectively
        //        super.setUp(3, spec);
        //
        //        // Only deploy on IND server
        //        Assert.assertNotNull(server1);
        //        Assert.assertNull(server2);
        //        Assert.assertNull(server3);
    }

    // Configure MicroService for OR scenario
    @Ignore("Modify code to remove region in replication for DHT and consider it in node selection")
    @Test
    public void testTwoRegionNodeSelection() throws Exception {
        //        MicroServiceSpec spec =
        //                MicroServiceSpec.newBuilder()
        //                        .setLang(Language.java)
        //                        .setJavaClassName("amino.run.sampleSO.SO")
        //                        .addDMSpec(
        //                                DMSpec.newBuilder()
        //                                        .setName(DHTPolicy.class.getName())
        //                                        .addConfig(config)
        //                                        .create())
        //                        .setNodeSelectorSpec(
        //                                new NodeSelectorSpec() // node Selector with OR expression
        //                                        .addRequireExpressions( // added expression for
        // use IND
        //                                                // region
        //                                                new NodeSelectorTerm()
        //                                                        .add(
        //                                                                new Requirement(
        //                                                                        REGION_KEY,
        //                                                                        Operator.Equal,
        //
        // Collections.singletonList(
        //                                                                                "IND"))))
        //                                        .addRequireExpressions( // added expression for
        // use CHN
        //                                                // region
        //                                                new NodeSelectorTerm()
        //                                                        .add(
        //                                                                new Requirement(
        //                                                                        REGION_KEY,
        //                                                                        Operator.Equal,
        //
        // Collections.singletonList(
        //                                                                                "CHN")))))
        //                        .create();
        //        serversInSameRegion = false;
        //        // setup 3 kernel server with IND, CHN and USA as region respectively
        //        super.setUp(3, spec);
        //
        //        // Only IND server object
        //        Assert.assertTrue(server1 != null || server2 != null);
        //        Assert.assertNull(server3);
    }
}
