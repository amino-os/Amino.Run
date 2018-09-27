package sapphire.app;

import org.junit.Assert;
import org.junit.Test;
import sapphire.common.Utils;
import sapphire.policy.scalability.LoadBalancedFrontendPolicy;

public class SapphireObjectSpecTest {

    @Test
    public void testToYamlFromYaml() throws Exception {
        LoadBalancedFrontendPolicy.Config config = new LoadBalancedFrontendPolicy.Config();
        config.setMaxConcurrentReq(200);
        config.setReplicaCount(30);

        SapphireObjectSpec soSpec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.js)
                        .setName("com.org.College")
                        .setSourceFileLocation("src/main/js/college.js")
                        .setConstructorName("college")
                        .addDM(Utils.toDMSpec(config))
                        .create();

        SapphireObjectSpec soSpecClone = SapphireObjectSpec.fromYaml(soSpec.toString());
        Assert.assertEquals(soSpec, soSpecClone);

        DMSpec dmSpec = soSpecClone.getDmList().get(0);
        LoadBalancedFrontendPolicy.Config configClone =
                (LoadBalancedFrontendPolicy.Config) Utils.toConfig(dmSpec);
        Assert.assertEquals(config, configClone);
    }
}
