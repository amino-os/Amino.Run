package amino.run.app;

import amino.run.common.Utils;
import amino.run.policy.scalability.LoadBalancedFrontendPolicy;
import amino.run.policy.scalability.ScaleUpFrontendPolicy;
import org.junit.Assert;
import org.junit.Test;

public class MicroServiceSpecTest {
    @Test
    public void testToYamlFromYaml() {
        MicroServiceSpec soSpec = createSpec();
        MicroServiceSpec soSpecClone = MicroServiceSpec.fromYaml(soSpec.toString());
        Assert.assertEquals(soSpec, soSpecClone);
    }

    @Test
    public void testSerializeEmptySpec() {
        MicroServiceSpec spec = MicroServiceSpec.newBuilder().create();
        MicroServiceSpec clone = MicroServiceSpec.fromYaml(spec.toString());
        Assert.assertEquals(spec, clone);
    }

    @Test
    public void testSerialization() throws Exception {
        MicroServiceSpec spec = createSpec();
        MicroServiceSpec clone = (MicroServiceSpec) Utils.toObject(Utils.toBytes(spec));
        Assert.assertEquals(spec, clone);
    }

    @Test
    public void testDmListEmptyValidation() throws Exception {
        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.js)
                        .setName("com.org.College")
                        .addDMSpec(null)
                        .create();
        spec.validate();
        Assert.assertTrue(spec.getDmList().isEmpty());
    }

    private MicroServiceSpec createSpec() {
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

        return MicroServiceSpec.newBuilder()
                .setLang(Language.js)
                .setName("com.org.College")
                .setSourceFileLocation("src/main/js/college.js")
                .setConstructorName("college")
                .addDMSpec(dmSpec)
                .setNodeSelectorSpec(new NodeSelectorSpec())
                .create();
    }
}
