package sapphire.appexamples.minnietwitter.cloud;

import sapphire.app.AppEntryPoint;
import sapphire.app.AppObjectNotCreatedException;
import static sapphire.runtime.Sapphire.*;
import sapphire.appexamples.minnietwitter.app.TwitterManager;
import sapphire.common.AppObjectStub;

public class MinnieTwitterStart implements AppEntryPoint {

	@Override
	public AppObjectStub start() throws AppObjectNotCreatedException {
			return (AppObjectStub) new_(TwitterManager.class);
	}
}
