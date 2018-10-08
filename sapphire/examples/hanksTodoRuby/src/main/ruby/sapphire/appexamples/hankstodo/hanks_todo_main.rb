#!/usr/bin/ruby

class HanksTodoMain

    def main
        # Set system property in for Java Stub
        system = Java.type("java.lang.System")
        system.setProperty("RUBY_HOME", ENV['RUBY_HOME'])

        # 1. Load TodoListManager_Stub class with GraalVM API.
        # Eventually we will get TodoListManager_Stub from OMS.
        omsClientClass = Java.type("sapphire.appexamples.hankstodo.stubs.Client_Stub")

        # create new instance
        omsClient = omsClientClass.new

        tlm = omsClient.GetTodoListManager(ENV['RUBY_IP'], ENV['RUBY_PORT'])

        # create new TODO list
        puts #{tlm.methods}"
        t1 = tlm.newTodoList("Hanks")
        puts "got the object #{t1}"
        # Add todo tasks in ist
        #puts "#{t1.addToDo("First todo")}"
        #puts "#{t1.addToDo("Second todo")}"
        #puts "#{t1.addToDo("Third todo")}"

        #t1.completeToDo("Second todo")

    end
end

HanksTodoMain.new.main
