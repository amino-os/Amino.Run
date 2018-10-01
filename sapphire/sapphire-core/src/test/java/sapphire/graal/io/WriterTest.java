package sapphire.graal.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.app.Language;

public class WriterTest {
    private Context polyglotCtx;
    private String JS_Code =
            "class Student {"
                    + "constructor() {"
                    + "this.id = 0;"
                    + "this.name = \"\";"
                    + "this.buddies = [];"
                    + "this.birthDate = new Date();"
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

    @Before
    public void setup() throws Exception {
        polyglotCtx = Context.newBuilder(new String[] {"js"}).allowAllAccess(true).build();
    }

    @Test
    public void testBasicSerialization() throws Exception {
        int studentId = 1;
        String studentName = "Alex";
        int buddyId = 2;
        String buddyName = "Bob";

        // Create a student
        Value Student = polyglotCtx.eval("js", JS_Code);
        Value student = Student.newInstance();
        student.getMember("setId").execute(studentId);
        student.getMember("setName").execute(studentName);

        // Create a buddy
        Value buddy = Student.newInstance();
        buddy.getMember("setId").execute(buddyId);
        buddy.getMember("setName").execute(buddyName);

        // Add buddy
        student.getMember("addBuddy").execute(buddy);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer ser = new Serializer(out, Language.js);

        Value clone = null;
        ser.serialize(student);
        ByteArrayInputStream io = new ByteArrayInputStream(out.toByteArray());

        Deserializer de = new Deserializer(io, polyglotCtx);
        clone = de.deserialize();
        Assert.assertEquals(studentId, clone.getMember("id").asInt());
        Assert.assertEquals(studentName, clone.getMember("name").asString());

        List<Object> list = clone.getMember("getBuddies").execute().as(List.class);
        Assert.assertEquals(1, list.size());
        Map m = (Map) list.get(0);
        Assert.assertEquals(buddyId, m.get("id"));
        Assert.assertEquals(buddyName, m.get("name"));
    }
}
