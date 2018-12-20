package sapphire.app;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import sapphire.common.LabelUtils;
import sapphire.common.Utils;
import sapphire.policy.scalability.LoadBalancedFrontendPolicy;
import sapphire.policy.scalability.ScaleUpFrontendPolicy;

public class SapphireObjectSpecTest {
    @Test
    public void testToYamlFromYaml() {
        SapphireObjectSpec soSpec = createSpec();
        SapphireObjectSpec soSpecClone = SapphireObjectSpec.fromYaml(soSpec.toString());
        Assert.assertEquals(soSpec, soSpecClone);
    }

    @Test
    public void testSerializeEmptySpec() {
        SapphireObjectSpec spec = SapphireObjectSpec.newBuilder().create();
        SapphireObjectSpec clone = SapphireObjectSpec.fromYaml(spec.toString());
        Assert.assertEquals(spec, clone);
    }

    @Test
    public void testSerialization() throws Exception {
        SapphireObjectSpec spec = createSpec();
        SapphireObjectSpec clone = (SapphireObjectSpec) Utils.toObject(Utils.toBytes(spec));
        Assert.assertEquals(spec, clone);
    }

    private SapphireObjectSpec createSpec() {
        NodeSelectorSpec nodeSelectorSpec = new NodeSelectorSpec();

        String[] values1 = {"val11", "val12", "val13"};
        String[] values2 = {"val21", "val22", "val23"};

        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem1 =
                new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals1);

        List<String> vals2 = Arrays.asList(values2);
        NodeSelectorRequirement matchExpItem2 =
                new NodeSelectorRequirement("key2", LabelUtils.NotIn, vals2);

        List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem1);

        List<NodeSelectorRequirement> MatchExpressions2 = Arrays.asList(matchExpItem2);

        NodeSelectorTerm term1 = new NodeSelectorTerm();
        term1.setMatchExpressions(MatchExpressions1);

        String[] values3 = {"val31"};
        List<String> vals3 = Arrays.asList(values3);
        NodeSelectorRequirement matchExpItem3 =
                new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals3);

        List<NodeSelectorRequirement> MatchFields = Arrays.asList(matchExpItem3);
        term1.setMatchFields(MatchFields);

        NodeSelectorTerm term2 = new NodeSelectorTerm();
        term2.setMatchExpressions(MatchExpressions2);

        PreferredSchedulingTerm prefterm = new PreferredSchedulingTerm();
        prefterm.setNodeSelectorTerm(term2);
        prefterm.setweight(1);

        List<PreferredSchedulingTerm> PreferSchedulingterms = Arrays.asList(prefterm);

        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term1);

        KsAffinity nodeAffinity = new KsAffinity();
        nodeAffinity.setPreferScheduling(PreferSchedulingterms);
        nodeAffinity.setRequireExpressions(RequireExpressions);

        nodeSelectorSpec.setNodeAffinity(nodeAffinity);

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

        return SapphireObjectSpec.newBuilder()
                .setLang(Language.js)
                .setName("com.org.College")
                .setSourceFileLocation("src/main/js/college.js")
                .setConstructorName("college")
                .addDMSpec(dmSpec)
                .setNodeSelectorSpec(nodeSelectorSpec)
                .create();
    }
}
