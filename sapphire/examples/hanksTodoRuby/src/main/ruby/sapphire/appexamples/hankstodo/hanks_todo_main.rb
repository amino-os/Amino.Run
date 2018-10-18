#!/usr/bin/ruby

class HanksTodoMain

    def main
        # Set system property in for Java Stub
        system = Java.type("java.lang.System")
        system.setProperty("RUBY_HOME", ENV['RUBY_HOME'])
        fileName = ENV['RUBY_HOME'] + "/todo_list_manager.rb"

        # 1. Load TodoListManager_Stub class with GraalVM API.
        # Eventually we will get TodoListManager_Stub from OMS.
        omsClientClass = Java.type("sapphire.appexamples.hankstodo.stubs.Client_Stub")

        # create new instance
        omsClient = omsClientClass.new
        tlm = omsClient.GetTodoListManager(ENV['KERNEL_SERVER_IP'], ENV['KERNEL_SERVER_PORT'], ENV['OMS_IP'], ENV['OMS_PORT'], "ruby", fileName)

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
