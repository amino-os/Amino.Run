package sapphire.app;

import sapphire.common.AppObjectStub;

public interface AppEntryPoint {
	public AppObjectStub start() throws AppObjectNotCreatedException;
}