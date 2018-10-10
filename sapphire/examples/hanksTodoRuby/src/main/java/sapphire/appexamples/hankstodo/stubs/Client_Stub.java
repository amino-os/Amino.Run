package sapphire.appexamples.hankstodo.stubs;

import sapphire.app.Language;

import java.net.InetSocketAddress;

public class Client_Stub {
    public Client_Stub(){}

    public sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub GetTodoListManager(
            InetSocketAddress host,
            InetSocketAddress omsHost,
            sapphire.app.Language lang,
            String fileName) {
        try {
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

        InetSocketAddress host, omsHost;

        try {
            host = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
            omsHost = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
        } catch (NumberFormatException e) {
            System.out.println("Incorrect arguments to the kernel server");
            System.out.println("[host ip] [host port] [oms ip] [oms port]");
            return;
        }

        java.lang.System.setProperty("RUBY_HOME", "/Users/haibinxie/Code/DCAP_Sapphire_Fork/DCAP-Sapphire/sapphire/examples/hanksTodoRuby/src/main/ruby/sapphire/appexamples/hankstodo");
        java.lang.String rubyHome = java.lang.System.getProperty("RUBY_HOME");

        Test(host, omsHost, sapphire.app.Language.js, rubyHome + "/todo_list_manager.js");
        Test(host, omsHost, Language.ruby, rubyHome + "/todo_list_manager.rb");
    }

    private static void Test(InetSocketAddress host, InetSocketAddress omsHost, sapphire.app.Language lang, String fileName) {
        Client_Stub client = new Client_Stub();
        sapphire.appexamples.hankstodo.stubs.TodoListManager_Stub tls =
                client.GetTodoListManager(host, omsHost, lang, fileName);

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
