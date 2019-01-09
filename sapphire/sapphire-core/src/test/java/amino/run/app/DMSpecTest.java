package amino.run.app;

import amino.run.common.Utils;
import amino.run.policy.scalability.LoadBalancedFrontendPolicy;
import amino.run.policy.scalability.ScaleUpFrontendPolicy;
import org.junit.Assert;
import org.junit.Test;

public class DMSpecTest {

    @Test
    public void testYaml() {
        DMSpec spec = createDMSpec();
        DMSpec clone = DMSpec.fromYaml(spec.toString());
        Assert.assertEquals(spec, clone);
    }

    @Test
    public void testSerialization() throws Exception {
        DMSpec spec = createDMSpec();
        DMSpec clone = (DMSpec) Utils.toObject(Utils.toBytes(spec));
        Assert.assertEquals(spec, clone);
    }

    private DMSpec createDMSpec() {
        ScaleUpFrontendPolicy.Config scaleUpConfig = new ScaleUpFrontendPolicy.Config();
        scaleUpConfig.setReplicationRateInMs(100);

        LoadBalancedFrontendPolicy.Config lbConfig = new LoadBalancedFrontendPolicy.Config();
        lbConfig.setMaxConcurrentReq(200);
        lbConfig.setReplicaCount(30);

        return DMSpec.newBuilder()
                .setName(ScaleUpFrontendPolicy.class.getName())
                .addConfig(scaleUpConfig)
                .addConfig(lbConfig)
                .create();
    }
}
