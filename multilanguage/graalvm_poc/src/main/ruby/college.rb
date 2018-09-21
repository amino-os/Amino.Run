#!/usr/bin/ruby

$LOAD_PATH << './src/main/ruby/'

require 'student'

class College
    def initialize(name)
        @name = name
        @students = []
    end

    def getName()
        return @name
    end

    def addStudent(student)
        @students.push(student)
    end

    def getStudents()
        return @students
    end
end