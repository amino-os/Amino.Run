package sapphire.appexamples.minnietwitter.app;

import java.util.ArrayList;
import java.util.List;

import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;

public class User implements SapphireObject {
	private Timeline timeline;
	private UserInfo ui;
	List<User> followers;
	List<User> following;
	TagManager tagManager;
	
	public User(UserInfo ui, TagManager tm) {
		this.ui = ui;
		this.followers = new ArrayList<User>();
		this.following = new ArrayList<User>();
		tagManager = tm;
	}

	public void initialize(User u) {
		timeline = (Timeline) new_(Timeline.class, u, tagManager);
		timeline.initialize(timeline);
	}
	
	public List<User> getFollowers(int from, int to) {
		return Util.checkedSubList(followers, from, to);
	}
	
	public List<User> getFollowing(int from, int to) {
		return Util.checkedSubList(following, from, to);
	}
	
	public UserInfo getUserInfo() {
		return ui;
	}
	
	public Timeline getTimeline() {
		return timeline;
	}
	
	public void addFollower(User u) {
		followers.add(u);
	}
	
	public void addFollowing(User u) {
		following.add(u);
	}
}
