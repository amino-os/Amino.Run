#!/usr/bin/ruby

class TodoList
    def initialize(name)
        @to_dos = Array.new
        @name = name
    end

    def addTodo(todo)
        @to_dos.push(todo)
        puts "add #{todo} success!!!"
        return "OK!"
    end

    def completeTodo(todo)
        @to_dos.delete(todo)
        puts "#{todo} task removed"
        return "OK!"
    end

    def getTodos
        return @to_dos
    end

    def getName
        @name
    end
end
