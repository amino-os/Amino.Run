package amino.run.appexamples.helloworld;

import amino.run.app.SapphireObject;
import amino.run.app.*;
import amino.run.runtime.SapphireConfiguration;

@SapphireConfiguration(Policies = "amino.run.policy.atleastoncerpc.AtLeastOnceRPCPolicy")
public class HelloWorld implements SapphireObject {
    private String world = "DCAP World";

    public HelloWorld(){}
    public HelloWorld(String world) {
        this.world = world;
    }

    public String sayHello() {
        return "Hi " + this.world;
    }
}
