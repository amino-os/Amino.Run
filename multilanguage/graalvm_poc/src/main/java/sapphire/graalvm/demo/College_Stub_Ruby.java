package sapphire.graalvm.demo;

import org.graalvm.polyglot.*;
import java.io.*;
import java.util.*;

// This is a stub file for Ruby College class.
// This file is hand coded at present.

/**
 * Stub for Ruby College class.
 * This stub exposes the same methods as the ruby
 * College class.
 */
public class College_Stub_Ruby {
    Context polyglot;
    Value college;

    public College_Stub_Ruby() throws Exception{
        // Use GraalVM polyplot API to create a College instance.
        polyglot = Context.newBuilder(new String[] {"ruby"})
                .allowAllAccess(true)
                .build();

        // Build the context from code with Ruby
        String rubyHome = System.getProperty("RUBY_HOME");
        Value v0 = polyglot.eval(Source.newBuilder("ruby", new File(rubyHome + "/college.rb")).build());

        // create College class instance for ruby
        college = polyglot.eval("ruby", "College").newInstance("RubyCollege");
    }

    public String getName() throws Exception {
        // 1. Use GraalVM polyplot API to invoke
        // getName method on the college instance. 
        // 2. Serialize return value to bytes
        // 3. Descerialize bytes into GraalVM Value object
        Value v = college.getMember("getName").execute();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sapphire.graalvm.serde.Serializer ser = new sapphire.graalvm.serde.Serializer(out, "ruby");
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
        Value clientStudent = polyglot.eval("ruby", "Student").newInstance();
        clientStudent.getMember("setId").execute(id);
        clientStudent.getMember("setName").execute(name);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sapphire.graalvm.serde.Serializer ser = new sapphire.graalvm.serde.Serializer(out, "ruby");
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
        sapphire.graalvm.serde.Serializer ser = new sapphire.graalvm.serde.Serializer(out, "ruby");
        ser.serialize(serverStudents);

        sapphire.graalvm.serde.Deserializer de = new sapphire.graalvm.serde.Deserializer(
                new ByteArrayInputStream(out.toByteArray()),
                polyglot);
        Value clientStudents = de.deserialize();
        return clientStudents.as(List.class);
    }
}
