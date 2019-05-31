package amino.run.appexamples.minnietwitter;

import static amino.run.runtime.MicroService.delete_;
import static amino.run.runtime.MicroService.new_;

import amino.run.app.MicroService;

public class TwitterManager implements MicroService {
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
