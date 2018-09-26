package sapphire.common;

import static org.junit.Assert.*;

import java.io.*;
import org.graalvm.polyglot.*;
import org.junit.Test;

public class ObjectHandlerTest {

    @Test
    public void testValue() throws Exception {
        Context c = Context.create();
        Value v = c.eval("js", "[1,42,3]");
        ObjectHandler objHandler = new ObjectHandler(v);
        objHandler.SetGraalContext(c);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
        oos.writeObject(objHandler);
        //        objHandler.write(oos);
        oos.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        //        ObjectHandler inObj = new ObjectHandler(c.eval("js", "0"));
        //        inObj.SetGraalContext(c);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputSteam = new ObjectInputStream(byteArrayInputStream);
        ObjectHandler clone = (ObjectHandler) objectInputSteam.readObject();
        //        inObj.read(objectInputSteam);

        System.out.println("Original value is " + objHandler.getGraalObject());
        System.out.println("After SerDe, value is " + objHandler.getGraalObject());

        assert objHandler.getGraalObject().toString().equals(clone.getGraalObject().toString());
    }
}
