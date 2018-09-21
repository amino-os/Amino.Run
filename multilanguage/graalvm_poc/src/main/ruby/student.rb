#!/usr/bin/ruby

class Student
    def initialize
        @name = ""
        @id = 0
    end

    def setName(name)
        @name = name
    end

    def setId(id)
        @id = id
    end

    def getName
        return @name
    end

    def getID
       return @id
    end
end