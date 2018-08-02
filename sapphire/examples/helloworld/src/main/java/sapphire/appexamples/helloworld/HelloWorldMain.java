package sapphire.appexamples.helloworld;

public class HelloWorldMain {
    public static void main(String[] args) {
        String world = "DCAP World";
        if (args.length > 0 && args[0] != null && args[0].length()>0) {
            world = args[0];
        }

        HelloWorld helloWorld = new HelloWorld(world);
        System.out.println(helloWorld.sayHello());
    }
}
