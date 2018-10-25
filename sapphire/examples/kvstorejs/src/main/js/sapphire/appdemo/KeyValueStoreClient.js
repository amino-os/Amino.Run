/**
 * Client for testing {@link KeyValueStore}
 */
function main() {
    // get init configurations
    var system = Java.type("java.lang.System")
    var hostIP = system.getenv("HOST_IP")
    var hostPort = system.getenv("HOST_PORT")
    var omsIP = system.getenv("OMS_IP")
    var omsPort = system.getenv("OMS_PORT")

    // DM config file
    var ClientStub = Java.type("sapphire.appdemo.stubs.KeyValueStore_Stub")
    var kvs = ClientStub.getStub("KeyValueStore.yaml", omsIP, omsPort, hostIP, hostPort);

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