package sapphire.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by SMoon on 6/4/2018.
 */
public class SapphireTest {
    static final String dm1Annotation = "testDM1", dm2Annotation = "testDM2";

    /**
     * Tests Sapphire run time for parsing annotation.
     * @throws Exception
     */
    @Test
    public void new_() throws Exception {
        Annotation[] annotations = TestBlah.class.getAnnotations();
        List<String> DMchain = new ArrayList<String>();

        for (Annotation annotation : annotations) {
            if (annotation instanceof SapphireConfiguration) {
                String[] DMannotations = ((SapphireConfiguration) annotation).DMs();
                for (String DMannotation : DMannotations) {
                    String[] DMs = DMannotation.split(",");
                    for (String DM : DMs) {
                        DMchain.add(DM);
                    }
                }
            }
        }

        Assert.assertEquals(dm1Annotation, DMchain.get(0));
        Assert.assertEquals(dm2Annotation, DMchain.get(1));
    }

    @SapphireConfiguration(DMs = {dm1Annotation + "," + dm2Annotation})
    public static class TestBlah {

    }
}