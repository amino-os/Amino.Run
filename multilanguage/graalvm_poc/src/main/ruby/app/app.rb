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
        # College college = ...
        collegeStubClass = Java.type("sapphire.graalvm.demo.College_Stub_Ruby")
        # create new instance
        college = collegeStubClass.new

        # get college name
        puts "app college name is : #{college.getName()}"

        # 2. Create a Student instance with GraalVM API.
        college.addStudent(1, "AmitStudent")
        college.addStudent(2, "AmitStudent2")

        # 3. Prints out all students from college
        collegeStuds = college.getStudents()
        collegeStuds.each { |student|
                  puts "#{student}"
                  puts "get student name #{student.getName}"
                  puts "get student id #{student.getID}"
        }
    end
end

AppRuby.new.main
