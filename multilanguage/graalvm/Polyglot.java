import org.graalvm.polyglot.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

class Polyglot {

	public static void main(String[] args) throws Exception {

      String[] supportedLangs = { "js", "python"};

        Context polyglot = Context.newBuilder(supportedLangs)
            .allowAllAccess(true)
            .build();
        Value v1 = polyglot.eval(Source.newBuilder("js", new File("./tests/arraydef.js")).build());
        Value v2 = polyglot.eval(Source.newBuilder("js", new File("./tests/classdef.js")).build());
				Value v3 = polyglot.asValue(new TestClass().mutate());
        Value v4 = polyglot.eval(Source.newBuilder("js", new File("./tests/complexclassdef.js")).build());
        //Value v5 = polyglot.eval(Source.newBuilder("python", new File("./tests/class.py")).build());
        //v5.getMember("mutate").execute();
        Value v6 = polyglot.asValue(ComplexClass.Init());

				TypesDB.register(v1);
				TypesDB.register(v2);
				TypesDB.register(v3);
        TypesDB.register(v4);
        TypesDB.register(v4.getMember("child1"));
        TypesDB.register(v4.getMember("child1").getMember("grandchild"));
        //TypesDB.register(v5);
        TypesDB.register(v6);
        TypesDB.register(v6.getMember("child1"));
        TypesDB.register(v6.getMember("child1").getMember("gc"));
        
				Value v = v6;

				System.out.println("--Writing--");
				Writer w = new Writer(new FileOutputStream("file.bin"));
				w.write(v);
				w.close();

				System.out.println();
				System.out.println();

				System.out.println("--Reading--");
				Reader r = new Reader(new FileInputStream("file.bin"), polyglot);
				Value vout = r.read();
				r.close();

				System.out.println();
				System.out.println();

				System.out.println(vout);
    }

	public static class TestClass {

    public static int global = 5;
		public String fielda = "test";
		private String fieldb = "test2"; //TODO no private fields allowed as of current

		public TestClass() {}

    public TestClass mutate() {
      fielda = "mutated";
      fieldb = "mutatedb";
      return this;
    }

    public TestClass construct() {
      return new TestClass();
    }

		public String toString() {
			return fielda + " " + fieldb;
		}
	}

  public static class ComplexClass extends ArrayList<Integer> {
    public InnerClass child1;
    public InnerClass child2;
    
    public ComplexClass construct() {
        return new ComplexClass();
    }
    public static ComplexClass Init() {
        ComplexClass out = new ComplexClass();
        out.child1 = new InnerClass();
        out.child2 = new InnerClass();
        out.child1.gc = new GrandchildClass();
        out.child2.gc = out.child1.gc;
        out.child1.gc.val = 5;
        return out;
    }
    public String toString() {
        return "gc equal? " + (child1.gc == child2.gc);
    }

    public static class InnerClass {
        public GrandchildClass gc;
        public InnerClass construct() {
            return new InnerClass();
        }
    }
    public static class GrandchildClass {
        public int val;
        public GrandchildClass construct() {
            return new GrandchildClass();
        }
    }
  }
}
