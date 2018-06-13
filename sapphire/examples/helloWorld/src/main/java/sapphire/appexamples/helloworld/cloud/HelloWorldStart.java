package sapphire.appexamples.helloworld.cloud;

import sapphire.app.AppEntryPoint;
import sapphire.app.AppObjectNotCreatedException;
import sapphire.appexamples.helloworld.app.HelloWorld;
import sapphire.runtime.Sapphire;
import sapphire.common.AppObjectStub;

public class HelloWorldStart implements AppEntryPoint {

	@Override
	public AppObjectStub start() throws AppObjectNotCreatedException {
			return (AppObjectStub) Sapphire.new_(HelloWorld.class, "Client Name");
	}
}
