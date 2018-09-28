package sapphire.app;

import org.junit.Assert;
import org.junit.Test;
import sapphire.policy.scalability.LoadBalancedFrontendPolicy;
import sapphire.policy.scalability.ScaleUpFrontendPolicy;

public class SapphireObjectSpecTest {

    @Test
    public void testToYamlFromYaml() {
        ScaleUpFrontendPolicy.Config scaleUpConfig = new ScaleUpFrontendPolicy.Config();
        scaleUpConfig.setReplicationRateInMs(100);

        LoadBalancedFrontendPolicy.Config lbConfig = new LoadBalancedFrontendPolicy.Config();
        lbConfig.setMaxConcurrentReq(200);
        lbConfig.setReplicaCount(30);

        DMSpec dmSpec =
                DMSpec.newBuilder()
                        .setName(ScaleUpFrontendPolicy.class.getName())
                        .addConfig(scaleUpConfig)
                        .addConfig(lbConfig)
                        .create();

        SapphireObjectSpec soSpec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.js)
                        .setName("com.org.College")
                        .setSourceFileLocation("src/main/js/college.js")
                        .setConstructorName("college")
                        .addDMSpec(dmSpec)
                        .create();

        System.out.println(soSpec.toString());

        SapphireObjectSpec soSpecClone = SapphireObjectSpec.fromYaml(soSpec.toString());
        Assert.assertEquals(soSpec, soSpecClone);

        for (DMSpec ds : soSpecClone.getDmList()) {
            ScaleUpFrontendPolicy.Config scaleUpConfigClone =
                    (ScaleUpFrontendPolicy.Config) ds.getConfigs().get(0);
            Assert.assertEquals(scaleUpConfig, scaleUpConfigClone);
            LoadBalancedFrontendPolicy.Config lbConfigClone =
                    (LoadBalancedFrontendPolicy.Config) ds.getConfigs().get(1);
            Assert.assertEquals(lbConfig, lbConfigClone);
        }
    }

    @Test
    public void testSerializeEmptySpec() {
        SapphireObjectSpec spec = SapphireObjectSpec.newBuilder().create();
        SapphireObjectSpec clone = SapphireObjectSpec.fromYaml(spec.toString());
        Assert.assertEquals(spec, clone);
    }
}
