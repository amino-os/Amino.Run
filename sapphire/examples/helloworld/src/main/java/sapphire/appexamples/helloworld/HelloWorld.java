package sapphire.appexamples.helloworld;

import sapphire.app.*;
import sapphire.common.AppObjectStub;
import sapphire.policy.atleastoncerpc.AtLeastOnceRPCPolicy;
import sapphire.runtime.Sapphire;

public class HelloWorld implements SapphireObject<AtLeastOnceRPCPolicy>, AppEntryPoint {
    private String world = "DCAP World";

    public HelloWorld(){}
    public HelloWorld(String world) {
        this.world = world;
    }

    public String sayHello() {
        return "Hi " + this.world;
    }

    @Override
    public AppObjectStub start() throws AppObjectNotCreatedException {
        return (AppObjectStub) Sapphire.new_(HelloWorld.class);
    }
}
