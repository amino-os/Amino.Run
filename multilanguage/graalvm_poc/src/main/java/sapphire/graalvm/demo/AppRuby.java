package sapphire.graalvm.demo;
import java.util.List;
public class AppRuby {
    // The purpose of this POC is to demonstrate that
    // a Java application is able to use a SO written
    // in Ruby.
    public static void main(String[] args) {
        try {
            // 1. Load College_Stub_Ruby class with GraalVM API.
            // Eventually we will get Student_Stub from OMS.
            // College college = ...
            College_Stub_Ruby college = new College_Stub_Ruby();
            String name = college.getName();
            System.out.println("Created College: " + name);

            // 2. Create a Student instance with GraalVM API.
            String s1 = "ruby_student_1";
            college.addStudent(1, s1);
            System.out.println(String.format("Add student %s to college.", s1));
            String s2 = "ruby_student_2";
            college.addStudent(2, s2);
            System.out.println(String.format("Add student %s to college.", s2));

            System.out.println("Getting students from college...");
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
