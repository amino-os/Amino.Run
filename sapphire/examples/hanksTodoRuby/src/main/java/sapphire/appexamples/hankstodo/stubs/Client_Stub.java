package sapphire.appexamples.hankstodo.stubs;

import sapphire.app.Language;

import java.net.InetSocketAddress;

public class Client_Stub {
    public Client_Stub(){}

    public sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub GetTodoListManager(
            String hostIp,
            String hostPort,
            String omsIp,
            String omsPort,
            String langStr,
            String fileName) {

        try {
            InetSocketAddress host = new InetSocketAddress(hostIp, Integer.parseInt(hostPort));
            InetSocketAddress omsHost = new InetSocketAddress(omsIp, Integer.parseInt(omsPort));
            sapphire.app.Language lang = Language.valueOf(langStr);

            // Create OMSClient for oms server interaction
            sapphire.app.OMSClient omsClient = new sapphire.app.OMSClient(host, omsHost);
            sapphire.policy.dht.DHTPolicy.Config config = new sapphire.policy.dht.DHTPolicy.Config();
            config.setNumOfShards(3);

            sapphire.app.SapphireObjectSpec spec = sapphire.app.SapphireObjectSpec.newBuilder()
                    .setLang(lang)
                    .setConstructorName("TodoListManager")
                    .setJavaClassName("sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub")
                    .setSourceFileLocation((new java.io.File(fileName).toString()))
                    .addDMSpec(sapphire.app.DMSpec.newBuilder()
                            .setName(sapphire.policy.dht.DHTPolicy.class.getName())
                            .addConfig(config)
                            .create())
                    .create();

            sapphire.common.SapphireObjectID sapphireObjId = omsClient.createSapphireObject(spec);
            sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub tlm =
                    (sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub) omsClient.acquireSapphireObjectStub(sapphireObjId);
            tlm.$__initializeGraal(spec);
            return tlm;
        } catch (java.lang.Exception e) {
            throw new java.lang.RuntimeException(e);
        }
    }

    public static void main(String... args){
        if (args.length < 4) {
            System.out.println("Incorrect arguments to the kernel server");
            System.out.println("[host ip] [host port] [oms ip] [oms port]");
            return;
        }

        String hostIp = args[0];
        String hostPort = args[1];
        String omsIp = args[2];
        String omsPort = args[3];

        java.lang.String rubyHome = java.lang.System.getProperty("RUBY_HOME");
        java.lang.String jsHome = java.lang.System.getProperty("JS_HOME");
        Test(hostIp, hostPort, omsIp, omsPort, sapphire.app.Language.js, jsHome + "/todo_list_manager.js");
        Test(hostIp, hostPort, omsIp, omsPort, Language.ruby, rubyHome + "/todo_list_manager.rb");
    }

    private static void Test(String hostIp, String hostPort, String omsIp, String omsPort, sapphire.app.Language lang, String fileName) {
        Client_Stub client = new Client_Stub();
        sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub tls =
                client.GetTodoListManager(hostIp, hostPort, omsIp, omsPort, lang.name(), fileName);

        java.lang.System.out.println(String.format("**************Test %s*************", lang));
        java.lang.System.out.println("Response from SO for newTodoList --> " + tls.newTodoList("Hanks"));
        java.lang.System.out.println("Response from SO for addTodo --> " + tls.addTodo("Hanks","Hanks first Task"));
        java.lang.System.out.println("Response from SO for addTodo --> " + tls.addTodo("Hanks","Hanks second Task"));
        java.lang.System.out.println("Response from SO for getTodos --> " + tls.getTodos("Hanks"));
        java.lang.System.out.println("Response from SO for completeTodo --> " + tls.completeTodo("Hanks","Hanks first Task" ));
        java.lang.System.out.println("Response from SO for getTodos --> " + tls.getTodos("Hanks"));
        java.lang.System.out.println("Response from SO for deleteTodoList --> " + tls.deleteTodoList("Hanks"));
    }
}
