package sapphire.appexamples.helloworld;

import sapphire.app.*;
import sapphire.runtime.SapphireConfiguration;

@SapphireConfiguration(Policies = "sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy")
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
