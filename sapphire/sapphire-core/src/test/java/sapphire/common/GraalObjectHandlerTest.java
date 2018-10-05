package sapphire.common;

import org.graalvm.polyglot.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class GraalObjectHandlerTest {

    @Test
    public void testValue() throws Exception {
        Context c = Context.create();
        Value v = c.eval("js", "[1,42,3]");
        ObjectHandler objHandler = new GraalObjectHandler(v);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
        oos.writeObject(objHandler);
        oos.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputSteam = new ObjectInputStream(byteArrayInputStream);
        ObjectHandler clone = (ObjectHandler) objectInputSteam.readObject();

        System.out.println("Original value is " + objHandler.getGraalObject());
        System.out.println("After SerDe, value is " + objHandler.getGraalObject());

        assert objHandler.getObject().toString().equals(clone.getObject().toString());
    }
}
