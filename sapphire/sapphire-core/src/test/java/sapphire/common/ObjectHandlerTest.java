package sapphire.common;

import static org.junit.Assert.*;

import java.io.*;
import org.graalvm.polyglot.*;
import org.junit.Test;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;

public class ObjectHandlerTest {

//    @Test
//    public void testGraalObject() throws Exception {
//        String filename = "./src/test/resources/student.js";
//
//        SapphireObjectSpec spec =
//                SapphireObjectSpec.newBuilder()
//                        .setLang(Language.js)
//                        .setConstructorName("Student")
//                        .setJavaClassName("sapphire.common.stubs.Student_Stub")
//                        .setSourceFileLocation(filename)
//                        .create();
//
//        sapphire.common.stubs.Student_Stub graalObject = new sapphire.common.stubs.Student_Stub();
//        graalObject.$__initializeGraal(spec, new Object[0]);
//        ObjectHandler objHandler = new ObjectHandler(graalObject);
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
//        oos.writeObject(objHandler);
//        oos.flush();
//
//        byte[] bytes = byteArrayOutputStream.toByteArray();
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
//        ObjectInputStream objectInputSteam = new ObjectInputStream(byteArrayInputStream);
//        ObjectHandler clone = (ObjectHandler) objectInputSteam.readObject();
//
//        System.out.println("Original value is " + objHandler.getObject().toString());
//        System.out.println("After SerDe, value is " + clone.getObject().toString());
//
//        assert objHandler.getObject().toString().equals(clone.getObject().toString());
//    }

    @Test
    public void testJavaObject() throws Exception {
        String s = "helloworld";
        ObjectHandler obj = new ObjectHandler(s);
        byte[] bytes = Utils.toBytes(obj);

        ObjectHandler obj2 = (ObjectHandler) Utils.toObject(bytes);

        assert s.equals(obj2.getObject());
    }
}
