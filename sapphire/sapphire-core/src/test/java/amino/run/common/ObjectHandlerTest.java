package amino.run.common;

import static org.junit.Assert.*;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import java.io.*;
import org.graalvm.polyglot.*;
import org.junit.Test;

public class ObjectHandlerTest {

    @Test
    public void testGraalObject() throws Exception {
        String filename = "./src/test/resources/student.js";

        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.js)
                        .setConstructorName("Student")
                        .setJavaClassName("amino.run.stubs.Student_Stub")
                        .setSourceFileLocation(filename)
                        .create();

        amino.run.stubs.Student_Stub graalObject = new amino.run.stubs.Student_Stub();
        graalObject.$__initializeGraal(spec, new Object[0]);
        ObjectHandler objHandler = new ObjectHandler(graalObject);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
        oos.writeObject(objHandler);
        oos.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputSteam = new ObjectInputStream(byteArrayInputStream);
        ObjectHandler clone = (ObjectHandler) objectInputSteam.readObject();

        System.out.println("Original value is " + objHandler.getObject().toString());
        System.out.println("After SerDe, value is " + clone.getObject().toString());

        assert objHandler.getObject().toString().equals(clone.getObject().toString());
    }

    @Test
    public void testJavaObject() throws Exception {
        String s = "helloworld";
        ObjectHandler obj = new ObjectHandler(s);
        byte[] bytes = Utils.toBytes(obj);

        ObjectHandler obj2 = (ObjectHandler) Utils.toObject(bytes);

        assert s.equals(obj2.getObject());
    }
}
