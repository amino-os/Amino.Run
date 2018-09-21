package sapphire.graalvm.demo;
import java.util.List;
public class AppRuby {
    // The purpose of this POC is to demonstrate that
    // a Java application is able to use a SO written
    // in Ruby.
    public static void main(String[] args) {
        try {
            System.out.println("******");
            // 1. Load College_Stub_Ruby class with GraalVM API.
            // Eventually we will get Student_Stub from OMS.
            // College college = ...
            College_Stub_Ruby college = new College_Stub_Ruby();
            String name = college.getName();
            System.out.println("app college name is " + name);

            // 2. Create a Student instance with GraalVM API.
            college.addStudent(1, "AmitStudent");
            college.addStudent(2, "AmitStudent2");

            // Prints out all students from college
            List<Object> students = college.getStudents();
            for (Object o: students) {
                System.out.println(o);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println(e.toString());
        }
    }
}
