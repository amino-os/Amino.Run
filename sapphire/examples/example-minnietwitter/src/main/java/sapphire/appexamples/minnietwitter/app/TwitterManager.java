package sapphire.appexamples.minnietwitter.app;

import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;

public class TwitterManager implements SapphireObject {
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
}
