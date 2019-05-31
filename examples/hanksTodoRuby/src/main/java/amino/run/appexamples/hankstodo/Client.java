package amino.run.appexamples.hankstodo;

import amino.run.app.Language;

public class Client {
    public static void main(String... args) throws Exception {
        if (args.length < 4) {
            System.out.println("Incorrect arguments to the kernel server");
            System.out.println("[host ip] [host port] [oms ip] [oms port]");
            return;
        }

        String hostIp = args[0];
        String hostPort = args[1];
        String omsIp = args[2];
        String omsPort = args[3];

        Test(hostIp, hostPort, omsIp, omsPort, amino.run.app.Language.js, "HanksTodoJS.yaml");
        Test(hostIp, hostPort, omsIp, omsPort, amino.run.app.Language.ruby, "HanksTodoRuby.yaml");
    }

    private static void Test(
            String hostIp,
            String hostPort,
            String omsIp,
            String omsPort,
            Language lang,
            String fileName)
            throws Exception {
        java.lang.System.out.println(String.format("**************Test %s*************", lang));
        amino.run.appexamples.hankstodo.stubs.TodoListManager_Stub tls;
        try {
            tls =
                    amino.run.appexamples.hankstodo.stubs.TodoListManager_Stub.getStub(
                            fileName, omsIp, omsPort, hostIp, hostPort);
        } catch (Exception e) {
            throw new java.lang.RuntimeException(e);
        }
        java.lang.System.out.println(
                "Response from SO for newTodoList --> " + tls.newTodoList("Hanks"));
        java.lang.System.out.println(
                "Response from SO for addTodo --> " + tls.addTodo("Hanks", "Hanks first Task"));
        java.lang.System.out.println(
                "Response from SO for addTodo --> " + tls.addTodo("Hanks", "Hanks second Task"));
        java.lang.System.out.println("Response from SO for getTodos --> " + tls.getTodos("Hanks"));
        java.lang.System.out.println(
                "Response from SO for completeTodo --> "
                        + tls.completeTodo("Hanks", "Hanks first Task"));
        java.lang.System.out.println("Response from SO for getTodos --> " + tls.getTodos("Hanks"));
        java.lang.System.out.println(
                "Response from SO for deleteTodoList --> " + tls.deleteTodoList("Hanks"));
        if (lang == Language.ruby)
            java.lang.System.out.println(
                    "Response from SO for testSpecialChar --> "
                            + tls.testSpacialCharInFunctionName$question$(
                                    "Test special char in function name"));
    }
}
