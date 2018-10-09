package sapphire.app;

import org.junit.Assert;
import org.junit.Test;
import sapphire.policy.scalability.LoadBalancedFrontendPolicy;
import sapphire.policy.scalability.ScaleUpFrontendPolicy;

public class DMSpecTest {

    @Test
    public void test() {
        ScaleUpFrontendPolicy.Config scaleUpConfig = new ScaleUpFrontendPolicy.Config();
        scaleUpConfig.setReplicationRateInMs(100);

        LoadBalancedFrontendPolicy.Config lbConfig = new LoadBalancedFrontendPolicy.Config();
        lbConfig.setMaxConcurrentReq(200);
        lbConfig.setReplicaCount(30);

        DMSpec spec =
                DMSpec.newBuilder()
                        .setName(ScaleUpFrontendPolicy.class.getName())
                        .addConfig(scaleUpConfig)
                        .addConfig(lbConfig)
                        .create();

        DMSpec clone = DMSpec.fromYaml(spec.toString());

        Assert.assertEquals(spec, clone);
    }
}
