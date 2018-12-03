package sapphire.app;

import java.util.*;
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

        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("key1", "value1");
        nodeSelectorSpec.setMatchLabels(matchLabels);

        nodeSelectorSpec.addMatchLabelsItem("and_label", "and_label");
        nodeSelectorSpec.addMatchLabelsItem("or_label", "or_label");

        List<NodeSelectorRequirement> matchExp = new ArrayList<NodeSelectorRequirement>();

        String[] values1 = {"val1", "val2", "val3"};
        List<String> vals1 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem1 =
                new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals1);

        List<String> vals2 = Arrays.asList(values1);
        NodeSelectorRequirement matchExpItem2 =
                new NodeSelectorRequirement("key1", LabelUtils.NotIn, vals2);

        List<NodeSelectorRequirement> MatchExpressions1 = Arrays.asList(matchExpItem1);

        List<NodeSelectorRequirement> MatchExpressions2 = Arrays.asList(matchExpItem2);

        NodeSelectorTerm term1 = new NodeSelectorTerm();
        term1.setMatchExpressions(MatchExpressions1);

        NodeSelectorTerm term2 = new NodeSelectorTerm();
        term2.setMatchExpressions(MatchExpressions2);

        PreferredSchedulingTerm prefterm = new PreferredSchedulingTerm();
        prefterm.setNodeSelectorTerm(term2);
        prefterm.setweight(1);

        List<PreferredSchedulingTerm> PreferSchedulingterms = Arrays.asList(prefterm);

        List<NodeSelectorTerm> RequireExpressions = Arrays.asList(term1);

        NodeAffinity nodeAffinity = new NodeAffinity();
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
