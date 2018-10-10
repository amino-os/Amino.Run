package sapphire.common;

import static org.junit.Assert.*;

import java.io.*;
import org.graalvm.polyglot.*;
import org.junit.Test;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.policy.dht.DHTPolicy;

public class ObjectHandlerTest {

    @Test
    public void testGraalObject() throws Exception {
        String JS_Code =
                "class Student {"
                        + "constructor() {"
                        + "this.id = 0;"
                        + "this.name = \"\";"
                        + "this.buddies = [];"
                        + "}"
                        + "setId(id) {"
                        + "this.id = id;"
                        + "}"
                        + "getId() {"
                        + "return this.id;"
                        + "}"
                        + "setName(name) {"
                        + "this.name = name;"
                        + "}"
                        + "getName() {"
                        + "return this.name;"
                        + "}"
                        + "addBuddy(buddy) {"
                        + "this.buddies.push(buddy);"
                        + "}"
                        + "getBuddies() {"
                        + "return this.buddies;"
                        + "}"
                        + "}";
        String filename = "student.js";

        PrintWriter out = new PrintWriter("student.js");
        out.println(JS_Code);
        out.flush();

        DHTPolicy.Config config = new DHTPolicy.Config();
        config.setNumOfShards(3);

        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.js)
                        .setConstructorName("Student")
                        .setSourceFileLocation(filename)
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(DHTPolicy.class.getName())
                                        .addConfig(config)
                                        .create())
                        .create();

        GraalObject graalObject = new GraalObject(spec, null);

        ObjectHandler objHandler = new ObjectHandler(graalObject);
        // objHandler.SetGraalContext(c);
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

        System.out.println("Original value is " + objHandler.getObject().toString());
        System.out.println("After SerDe, value is " + clone.getObject().toString());

        (new File(filename)).delete();

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
