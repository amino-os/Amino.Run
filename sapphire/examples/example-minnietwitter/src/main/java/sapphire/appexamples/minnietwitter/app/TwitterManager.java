package sapphire.appexamples.minnietwitter.app;

public class TwitterManager {
	private UserManager userManager;
	private TagManager tagManager;
	
	public TwitterManager() {
		tagManager = (TagManager) new TagManager();
		userManager = (UserManager) new UserManager(tagManager);
	}

	public UserManager getUserManager() {
		return userManager;
	}

	public TagManager getTagManager() {
		return tagManager;
	}	
}


