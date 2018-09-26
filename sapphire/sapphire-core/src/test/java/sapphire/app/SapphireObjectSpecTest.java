package sapphire.app;

import org.junit.Assert;
import org.junit.Test;

public class SapphireObjectSpecTest {

    @Test
    public void testToYamlFromYaml() {
        SapphireObjectSpec spec = new SapphireObjectSpec();
        spec.setLang(SapphireObjectSpec.Language.JS);
        spec.setName("com.org.College");
        spec.setSourceFileLocation("src/main/js/college.rb");
        spec.setConstructorName("college");

        DMSpec dm1 = new DMSpec();
        dm1.setName("DHT");
        dm1.addProperty("numOfReplicas", "5");

        DMSpec dm2 = new DMSpec();
        dm1.setName("MasterSlave");
        dm1.addProperty("numOfReplicas", "2");

        spec.addDM(dm1);
        spec.addDM(dm2);

        System.out.println(spec);
        SapphireObjectSpec clone = SapphireObjectSpec.fromYaml(spec.toString());
        Assert.assertEquals(spec, clone);
    }

    @Test
    public void testFromYaml() {
        String yamlStr =
                "{constructorName: college, javaClassName: null, "
                        + "lang: JS, name: com.org.College, sourceFileLocation: src/main/js/college.rb}";
    }
}
