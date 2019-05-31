package amino.run.appexamples.helloworld;

import amino.run.app.MicroService;
import amino.run.runtime.MicroServiceConfiguration;

@MicroServiceConfiguration(Policies = "amino.run.policy.atleastoncerpc.AtLeastOnceRPCPolicy")
public class HelloWorld implements MicroService {
    private String world = "World";

    public HelloWorld() {}

    public HelloWorld(String world) {
        this.world = world;
    }

    public String sayHello() {
        return "Hi " + this.world;
    }
}
