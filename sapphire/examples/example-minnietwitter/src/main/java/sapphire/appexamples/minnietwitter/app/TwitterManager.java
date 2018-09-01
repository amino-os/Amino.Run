package sapphire.appexamples.minnietwitter.app;

import sapphire.app.AbstractSapphireObject;
import static sapphire.runtime.Sapphire.*;

public class TwitterManager extends AbstractSapphireObject {
	private UserManager userManager;
	private TagManager tagManager;
	
	public TwitterManager() {
		tagManager = (TagManager) new_(TagManager.class);
		userManager = (UserManager) new_(UserManager.class, tagManager);
	}

	public UserManager getUserManager() {
		return userManager;
	}

	public TagManager getTagManager() {
		return tagManager;
	}

	public void deInitialize() {
		delete_(tagManager);
		delete_(userManager);
	}

	@Override
	public boolean getStatus() {
		return true;
	}
}
