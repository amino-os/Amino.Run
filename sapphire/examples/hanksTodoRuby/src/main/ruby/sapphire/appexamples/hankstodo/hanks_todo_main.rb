#!/usr/bin/ruby

class HanksTodoMain

    def main
        # Set system property in for Java Stub
        system = Java.type("java.lang.System")
        system.setProperty("RUBY_HOME", ENV['RUBY_HOME'])

        # 1. Load TodoListManager_Stub class with GraalVM API.
        # Eventually we will get TodoListManager_Stub from OMS.
        listManagerStubClass = Java.type("sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub")

        # create new instance
        tlm = listManagerStubClass.new

        # create new TODO list
        t1 = tlm.newTodoList("Hanks")
        # Add todo tasks in ist
        puts "#{t1.addToDo("First todo")}"
        puts "#{t1.addToDo("Second todo")}"
        puts "#{t1.addToDo("Third todo")}"

        t1.completeToDo("Second todo")

    end

end

HanksTodoMain.new.main
