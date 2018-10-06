#!/usr/bin/ruby

$LOAD_PATH << './src/main/ruby/sapphire/appexamples/hankstodo'

require "todo_list"

class Manager

    def initialize
        @todo_lists = Hash.new
    end

    def getList(name)
        return @todo_lists[name]
    end
end

class TodoListManager < Manager
    def new_todo_list(name)
        list = @todo_lists[name]
        if list == nil
            #list = TodoList.new
            list = Java.type("sapphire.appexamples.hankstodo.stubs.TodoList_Stub").new(name)
            @todo_lists[name] = list
        end

        puts "Create new list"
        puts "This managers lists" + @todo_lists.to_s
        return list
    end
end