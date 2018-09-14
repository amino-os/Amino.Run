package sapphire.graalvm.serde;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class JSIntrospectionTest {
    private Context polyglot;
    private String JS_Code =
        "class Student {" +
                "constructor() {" +
                "this.id = 0;" +
                "this.name = \"\";" +
                "this.buddies = [];" +
                "}" +
                "setId(id) {" +
                "this.id = id;" +
                "}" +
                "getId() {" +
                "return this.id;" +
                "}" +
                "setName(name) {" +
                "this.name = name;" +
                "}" +
                "getName() {" +
                "return this.name;" +
                "}" +
                "addBuddy(buddy) {" +
                "this.buddies.push(buddy);" +
                "}" +
                "getBuddies() {" +
                "return this.buddies;" +
                "}" +
                "}";

    @Before
    public void setup() {
        polyglot = Context.newBuilder(new String[] {"js"})
                .allowAllAccess(true)
                .build();
    }

    @Test
    public void testJSIntrospection() {
        int studentId = 1;
        String studentName = "Alex";
        int buddyId = 2;
        String buddyName = "Bob";

        // Create a student
        Value Student = polyglot.eval("js", JS_Code);
        Value student = Student.newInstance();
        student.getMember("setId").execute(studentId);
        student.getMember("setName").execute(studentName);

        // Create a buddy
        Value buddy = Student.newInstance();
        buddy.getMember("setId").execute(buddyId);
        buddy.getMember("setName").execute(buddyName);

        // Add buddy
        student.getMember("addBuddy").execute(buddy);

        // Verification
        Assert.assertEquals(studentId, student.getMember("id").asInt());
        Assert.assertEquals(studentName, student.getMember("name").asString());

        List<Object> list = student.getMember("getBuddies").execute().as(List.class);
        Assert.assertEquals(1, list.size());
        Map m = (Map)list.get(0);
        Assert.assertEquals(buddyId, m.get("id"));
        Assert.assertEquals(buddyName, m.get("name"));
    }
}
