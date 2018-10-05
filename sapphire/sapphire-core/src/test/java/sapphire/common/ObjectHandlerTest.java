package sapphire.common;

import static org.junit.Assert.*;

import java.io.*;
import org.junit.Test;

public class ObjectHandlerTest {
    @Test
    /**
     * Test a String
     * TODO: quinton: This was called testJavaObject, but it only really tersted a java String.
     *      We need to test an arbitrary java object, with inheritance, enbedded objects etc.
     */
    public void testJavaString() throws Exception {
        String s = "helloworld";
        ObjectHandler obj = new ObjectHandler(s);
        byte[] bytes = Utils.toBytes(obj);
        ObjectHandler obj2 = (ObjectHandler) Utils.toObject(bytes);
        assert s.equals(obj2.getObject());
    }
}
