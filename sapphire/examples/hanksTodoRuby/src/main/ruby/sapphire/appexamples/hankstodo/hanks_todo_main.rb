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
        puts #{tlm.methods}"
        t1 = tlm.newTodoList("Hanks")
        puts "got the object #{t1}"

        puts "#{tlm.addTodo("Hanks", "First todo")}"
        puts "#{tlm.addTodo("Hanks", "Second todo")}"
        puts "#{tlm.addTodo("Hanks", "Third todo")}"
        puts "#{tlm.getTodos("Hanks")}"
        puts "#{tlm.completeTodo("Hanks", "Second todo")}"
        puts "#{tlm.getTodos("Hanks")}"
    end
end

HanksTodoMain.new.main
