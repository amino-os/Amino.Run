function main() {
    system = Java.type("java.lang.System")
    var hostIP = system.getenv("HOST_IP")
    var hostPort = system.getenv("HOST_PORT")
    var omsIP = system.getenv("OMS_IP")
    var omsPort = system.getenv("OMS_PORT")

    var ClientStub = Java.type("sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub")
    var tlm = ClientStub.getStub("HanksTodoJS.yaml", omsIP, omsPort, hostIP, hostPort);

    var t1 = tlm.newTodoList("Hanks")
    console.log(tlm.addTodo("Hanks", "First todo"));
    console.log(tlm.addTodo("Hanks", "Second todo"));
    console.log(tlm.addTodo("Hanks", "Third todo"));
    console.log(tlm.getTodos("Hanks"));
    console.log(tlm.completeTodo("Hanks", "Second todo"));
    console.log(tlm.getTodos("Hanks"));
}

main();