import java.util.*;
import java.io.*;
import org.graalvm.polyglot.*;

public class Generator {

	/*public static void main(String[] args) throws Exception {
		Context polyglot = Context.create();
	        Value prototype = polyglot.eval("js", "[1,2,42,4]");
		Generator test = new Generator(prototype);
		test.generateClientStub();
    test.generateServerStub();
	}*/
  
	public static void main(String[] args) throws Exception {
    //src folder, package name, dest folder
    String[] supportedLangs = { "js", "python", "ruby" };
    Context polyglot = Context.newBuilder(supportedLangs).allowAllAccess(true).build();
    polyglot.eval("js", "[1,2,42,4]");
    Value v2 = polyglot.eval(Source.newBuilder("js", new File("./tests/classdef.js")).build());
    //Value v3 = polyglot.eval(Source.newBuilder("python", new File("./tests/class.py")).build());
    Value v4 = polyglot.eval(Source.newBuilder("ruby", new File("./tests/class.rb")).build());
    //Value v = polyglot.getBindings("js");
    //System.out.println(v.getMemberKeys());
    //System.out.println(v.getMember("testjs").newInstance());
    //Value v = polyglot.getBindings("python");
    //Value test = v.getMember("Pythontest");
    Value v = polyglot.getBindings("ruby");
    System.out.println(v.getMember("class"));
    System.out.println(v4);
  }

	public Generator(Value prototype) {
		this.prototype = prototype;
	}

	private PrintStream out;
	private Value prototype;
  private int indentLevel;
	public void generateClientStub() throws FileNotFoundException {
		//TODO directory for package name
		this.out = new PrintStream(className() + "ClientStub.java");
		if(prototype.isHostObject()) {
			generateJava();
		} else {
			generateGuest();
		}
	}

	public void generateGuest() {
    //TODO package
    println("import org.graalvm.polyglot.*");
    println("import java.io.ByteArrayOutputStream;");

		println("public final class " + className() + "ClientStub {"); //TODO implements interface?
    indentLevel++;
    println("public long oid;");
    println("public Context c;");
		for(String m : prototype.getMemberKeys()) {
			if(prototype.getMember(m).canExecute()) {
				println("public Object " + m + "(Object... args) {");
        indentLevel++;
        println("ByteArrayOutputStream bos = new ByteArrayOutputStream();");
        println("new Writer(bos).write(c.asValue(args));");
        println("byte[] params = bos.toByteArray();");
        println(""); //invoke on kernel with oid, m and params
        indentLevel--;
				println("}");
			}	
		}
    indentLevel--;
		println("}");
	}

	//we can get arg types for java
	public void generateJava() {
      //TODO: for now we just use weak typing
      generateGuest();
	}

  public void generateServerStub() throws FileNotFoundException {
      //TODO directory for package name
		  this.out = new PrintStream(className() + "ServerStub.java");
      //TODO appobject package
      println("import org.graalvm.polyglot.*");
      println("import java.io.ByteArrayOutputStream;");

      println("public final class " + className() + "ServerStub implements AppObject {");
      indentLevel++;
      println("public Value obj;");
      println("public Context c;");

      println("public byte[] invoke(String method, byte[] parambytes) {");
      indentLevel++;
      println("Object[] params = (Object[])new Reader(new ByteArrayInputStream(params), c).read().asHostObject();");
      println("Value ret;");
      println("switch(method) {");
      indentLevel++;
      for(String m : prototype.getMemberKeys()) {
          if(prototype.getMember(m).canExecute()) {
              println("case \"" + m + "\":");
              println("ret = obj.getMember(" + m + ").execute(params);");
              println("break;");
          }
      }
      indentLevel--;
      println("}"); //switch
      println("ByteArrayOutputStream bos = new ByteArrayOutputStream();");
      println("new Writer(bos).write(ret);");
      println("return bos.toByteArray();");
      indentLevel--;
      println("}"); //invoke

      indentLevel--;
      println("}"); //class
  }

	private String packageName() {
		String fullname = prototype.getMetaObject().toString();
		return fullname.substring(0, fullname.lastIndexOf(".") + 1).trim();
	}

	private String className() {
		String fullname = prototype.getMetaObject().toString();
		return fullname.substring(fullname.lastIndexOf(".") + 1).trim();
	}

  private void println(String s) {
      String[] lines = s.split("\n");
      String tabs = "\t\t\t\t\t\t\t\t\t\t\t\t".substring(0,indentLevel);

      for(String l : lines) {
          out.println(tabs + l);
      }
  }
}
