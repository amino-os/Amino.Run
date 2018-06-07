package sapphire.runtime;

import org.junit.Test;

import java.lang.annotation.Annotation;


/**
 * Created by SMoon on 6/4/2018.
 */
public class SapphireTest {

    @Test
    public void new_() throws Exception {
        TestBlah tt = new TestBlah();
        Annotation a = tt.getClass().getAnnotation(SapphireConfiguration.class);
        Annotation[] ann = TestBlah.class.getAnnotations();
        System.out.println(a);
    }


    @SapphireConfiguration(DMs = {"DMChainRetryPolicy, DMChainExplicitMigrationPolicy"})
    public static class TestBlah {

    }
}