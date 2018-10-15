package sapphire.graalvm.demo;

import org.graalvm.polyglot.*;
import java.io.*;
import java.util.*;

// This is a stub file for javascript College class.
// This file is hand coded at present. Eventually it
// will be generated automatically by Isaac's generator. 

/**
 * Stub for javascript College class.
 * This stub exposes the same methods as the javascript
 * College class.
 */
public class College_Stub_JS {
    Context polyglot;
    Value college;

    public College_Stub_JS() throws Exception{
        // Use GraalVM polyplot API to create a College instance.
        polyglot = Context.newBuilder(new String[] {"js"})
                .allowAllAccess(true)
                .build();

        String jsHome = System.getProperty("JS_HOME");
        Value v0 = polyglot.eval(Source.newBuilder("js", new File(jsHome + "/college.js")).build());
        college = polyglot.eval("js", "new College(\"JS_College\")");
    }

    public String getName() throws Exception {
        // 1. Use GraalVM polyplot API to invoke
        // getName method on the college instance. 
        // 2. Serialize return value to bytes
        // 3. Descerialize bytes into GraalVM Value object
        Value v = college.getMember("getName").execute();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sapphire.graalvm.serde.Serializer ser = new sapphire.graalvm.serde.Serializer(out, "js");
        ser.serialize(v);

        sapphire.graalvm.serde.Deserializer de = new sapphire.graalvm.serde.Deserializer(
                new ByteArrayInputStream(out.toByteArray()),
                polyglot);
        Value name = de.deserialize();
        return name.asString();
    }

    public void addStudent(int id, String name) throws Exception{
        // 1. Serialize student into bytes
        // 2. Deserialize bytes back into GraalVM Value object
        // 3. Use GraalVM polyplot API to invoke
        //    addStudent method on college.

        // Note: if the input parameters is non-primitive this would not work.
        //String s = String.format("new Student(%d, \"%s\")", id, name);
        Value clientStudent = polyglot.eval("js", "new Student()");
        clientStudent.getMember("setId").execute(id);
        clientStudent.getMember("setName").execute(name);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sapphire.graalvm.serde.Serializer ser = new sapphire.graalvm.serde.Serializer(out, "js");
        ser.serialize(clientStudent);

        sapphire.graalvm.serde.Deserializer de = new sapphire.graalvm.serde.Deserializer(
                new ByteArrayInputStream(out.toByteArray()),
                polyglot);
        Value serverStudent = de.deserialize();

        college.getMember("addStudent").execute(serverStudent);
    }

    public List<Object> getStudents() throws Exception{
        // 1. Use GraalVM polyplot API to invoke 
        // getName method on the college instance. 
        // 2. Serialize return value to bytes
        // 3. Descerialize bytes into GraalVM Value object
        Value serverStudents = college.getMember("getStudents").execute();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sapphire.graalvm.serde.Serializer ser = new sapphire.graalvm.serde.Serializer(out, "js");
        ser.serialize(serverStudents);

        sapphire.graalvm.serde.Deserializer de = new sapphire.graalvm.serde.Deserializer(
                new ByteArrayInputStream(out.toByteArray()),
                polyglot);
        Value clientStudents = de.deserialize();
        return clientStudents.as(List.class);
    }
}
