#!/usr/bin/ruby

$LOAD_PATH << './src/main/ruby/'

require 'student'

class AppRuby
    def main
        # Set system property in for Java Stub
        system = Java.type("java.lang.System")
        system.setProperty("RUBY_HOME", ENV['RUBY_HOME'])

        # 1. Load College_Stub_Ruby class with GraalVM API.
        # Eventually we will get Student_Stub from OMS.
        collegeStubClass = Java.type("sapphire.graalvm.demo.College_Stub_Ruby")

        # create new instance
        college = collegeStubClass.new

        # get college name
        puts "Created College: #{college.getName()}"

        # 2. Create a Student instance with GraalVM API.
        s1 = "ruby_student_1"
        puts "Add student #{s1} to college."
        college.addStudent(1, s1)

        s2 = "ruby_student_2"
        puts "Add student #{s2} to college."
        college.addStudent(2, s2)

        # 3. Prints out all students from college
        puts "Getting students from college..."
        collegeStuds = college.getStudents()
        collegeStuds.each { |student|
              puts "Got student: #{student.getName}"
        }
    end
end

AppRuby.new.main
