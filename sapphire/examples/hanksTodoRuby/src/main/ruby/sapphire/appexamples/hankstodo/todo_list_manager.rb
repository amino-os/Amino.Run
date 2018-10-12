#!/usr/bin/ruby
# TODO: Support multifile SO .
# Ruby library/file load steps:
#   loadPath = File.expand_path('src/main/ruby/sapphire/appexamples/hankstodo',__dir__)
#   $:.unshift(folder) unless $:.include?(loadPath)
# Identified Issue: When Ruby SO is used in kernel server "__dir__" returns current working directory
#                   But when run in Ruby env it returns directory of ruby file itself.
class TodoList
    def initialize(name)
        @to_dos = Array.new
        @name = name
    end

    def add_todo(todo)
        @to_dos.push(todo)
        puts "add #{todo} success!!!"
        return "OK!"
    end

    def complete_todo(todo)
        @to_dos.delete(todo)
        puts "#{todo} task removed"
        return "OK!"
    end

    def get_todos
        return @to_dos
    end

    def getName
        @name
    end
end

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