/**
 * Client class for testing {@link KeyValueStore}
 */

function main() {
    system = Java.type("java.lang.System")
    var jsHome = system.getenv("JS_HOME")
    var kernelServerIP = system.getenv("KERNEL_SERVER_IP")
    var kernelServerPort = system.getenv("KERNEL_SERVER_PORT")
    var omsIP = system.getenv("OMS_IP")
    var omsPort = system.getenv("OMS_PORT")

    system.setProperty("JS_HOME", jsHome)
    var fileName = jsHome + "/KeyValueStore.js"

    var ClientStub = Java.type("sapphire.demo.stubs.Client_Stub")
    var clientStub = new ClientStub()
    var kvs = clientStub.GetKeyValueStore(kernelServerIP, kernelServerPort, omsIP, omsPort, "js", fileName);

    for (i=0; i<30; ++i) {
        key = "key_" + i;
        val = "val_" + i;

        console.log(`client: setting ${key} = ${val}`)
        kvs.set(key, val)

        var v = kvs.get(key)
        console.log(`client: got value ${v} with key ${key}`)
    }
}

main();

