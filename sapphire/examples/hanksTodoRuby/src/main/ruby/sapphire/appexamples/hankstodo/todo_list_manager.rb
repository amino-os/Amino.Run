#!/usr/bin/ruby

folder = File.expand_path('src/main/ruby/sapphire/appexamples/hankstodo',__dir__)
puts "foldername : #{folder}"
$:.unshift(folder) unless $:.include?(folder)

require 'todo_list'

class Manager
    def initialize
        @todo_lists = Array.new
    end

    def getList(name)
        return @todo_lists[name]
    end
end

class TodoListManager < Manager
    def new_todo_list(name)
        list = nil
        @todo_lists.each { |lst|
            if lst.getName == name
                list = lst
            end
        }

        if list == nil
            list = TodoList.new(name)
            @todo_lists << list
        end

        puts "Create list"
        return name
    end

    def delete_todo_list(name)
        list = nil
        @todo_lists.each { |lst|
            if lst.getName == name
                list = lst
            end
        }

        if list != nil
            @todo_lists.delete(list)
        end

        puts "List Deleted"
        return "OK!!!!"
    end

    def add_todo(name, todo)
        list = nil
        @todo_lists.each { |lst|
        puts "list name #{lst.getName}"
            if lst.getName == name
                puts "find list success !!!"
                list = lst
            end
        }

        if list != nil
            return list.add_todo(todo)
        end
        return "Failed !!!!"
    end

    def get_todos(name)
        list = nil
        @todo_lists.each { |lst|
            if lst.getName == name
                puts "find list success !!!"
                list = lst
            end
        }

        if list != nil
            return list.get_todos
        end

        return nil
    end

    def complete_todo(name, todo)
        list = nil
        @todo_lists.each { |lst|
            if lst.getName == name
                puts "find list success !!!"
                list = lst
            end
        }

        if list != nil
            return list.complete_todo(todo)
        end

        return nil
    end
end