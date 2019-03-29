package amino.run.common;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class VarargsFunctionReflectionTest {

    @Test
    public void test() throws Exception {
        Map<String, Method> methodMap = new HashMap<String, Method>();
        VarargsFunctionReflectionTest object = new VarargsFunctionReflectionTest();
        for (Method m : object.getClass().getMethods()) {
            methodMap.put(m.toGenericString(), m);
            System.out.println("METHOD: " + m.toGenericString());
        }

        Method m =
                methodMap.get(
                        "public java.lang.String amino.run.common.VarargsFunctionReflectionTest.argConcatination(java.lang.Object...)");
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
