package sapphire.appexamples.hankstodo.stubs;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class VarargsFunctionReflectionTest {

    @Test
    public void test() throws Exception {
        Map<String, Method> methodMap = new HashMap<>();
        VarargsFunctionReflectionTest object = new VarargsFunctionReflectionTest();
        for (Method m : object.getClass().getMethods()) {
            methodMap.put(m.toGenericString(), m);
        }

        Method m =
                methodMap.get(
                        "public java.lang.String sapphire.appexamples.hankstodo.stubs.VarargsFunctionReflectionTest.argConcatination(java.lang.Object...)");
        Object ret = m.invoke(object, new Object[] {new String[] {"a", "s", "d"}});
        System.out.println(ret);
        Assert.assertNotNull(ret);
    }

    public String argConcatination(Object... args) {
        String ret = "";
        for (Object o : args) {
            ret += o;
        }
        return ret;
    }
}
