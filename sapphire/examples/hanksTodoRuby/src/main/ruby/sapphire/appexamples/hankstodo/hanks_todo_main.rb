#!/usr/bin/ruby

class HanksTodoMain

    def main
        # 1. Load TodoListManager_Stub class with GraalVM API.
        omsClientClass = Java.type("sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub")

        # get Stub
        tlm = omsClientClass.getStub("HanksTodoRuby.yaml", ENV['OMS_IP'], ENV['OMS_PORT'], ENV['KERNEL_SERVER_IP'], ENV['KERNEL_SERVER_PORT'])

        # create new TODO list
        t1 = tlm.newTodoList("Hanks")
        puts "got the object #{t1}"

        # add tasks
        puts "add First task #{tlm.addTodo("Hanks", "First todo")}"
        puts "add Second task #{tlm.addTodo("Hanks", "Second todo")}"
        puts "add Third task #{tlm.addTodo("Hanks", "Third todo")}"

        # get added tasks
        list = tlm.getTodos("Hanks")
        puts "Current Task List:"
        list.each do |task|
          puts "#{task}"
        end

        # update completed task
        puts "Update Second task status #{tlm.completeTodo("Hanks", "Second todo")}"

        # get pending tasks
        list = tlm.getTodos("Hanks")
        puts "Current Task List:"
        list.each do |task|
           puts "#{task}"
        end
    end
end

HanksTodoMain.new.main
