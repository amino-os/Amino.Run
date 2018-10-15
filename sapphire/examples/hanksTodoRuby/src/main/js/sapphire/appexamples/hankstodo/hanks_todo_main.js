function main() {
    system = Java.type("java.lang.System")
    var jsHome = system.getenv("JS_HOME")
    var kernelServerIP = system.getenv("KERNEL_SERVER_IP")
    var kernelServerPort = system.getenv("KERNEL_SERVER_PORT")
    var omsIP = system.getenv("OMS_IP")
    var omsPort = system.getenv("OMS_PORT")

    system.setProperty("JS_HOME", jsHome)
    var fileName = jsHome + "/todo_list_manager.js"

    var ClientStub = Java.type("sapphire.appexamples.hankstodo.stubs.Client_Stub")
    var clientStub = new ClientStub()
    var tlm = clientStub.GetTodoListManager(kernelServerIP, kernelServerPort, omsIP, omsPort, "js", fileName);

    var t1 = tlm.newTodoList("Hanks")
    console.log(tlm.addTodo("Hanks", "First todo"));
    console.log(tlm.addTodo("Hanks", "Second todo"));
    console.log(tlm.addTodo("Hanks", "Third todo"));
    console.log(tlm.getTodos("Hanks"));
    console.log(tlm.completeTodo("Hanks", "Second todo"));
    console.log(tlm.getTodos("Hanks"));
}

main();