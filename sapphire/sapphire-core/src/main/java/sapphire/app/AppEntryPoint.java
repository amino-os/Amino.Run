package sapphire.app;

import sapphire.common.AppObjectStub;

@Deprecated
public interface AppEntryPoint {
    public AppObjectStub start() throws AppObjectNotCreatedException;
}
