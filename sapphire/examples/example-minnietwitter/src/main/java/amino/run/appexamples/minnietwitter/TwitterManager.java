package amino.run.appexamples.minnietwitter;

import amino.run.app.SapphireObject;

import static amino.run.runtime.Sapphire.delete_;
import static amino.run.runtime.Sapphire.new_;

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

    public void deInitialize() {
        delete_(tagManager);
        delete_(userManager);
    }
}
