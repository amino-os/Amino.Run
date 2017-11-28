package sapphire.appexamples.minnietwitter.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Tag implements Serializable {
	private String label;
	private List<Tweet> tweets;
	
	public Tag(String label) {
		this.label = label;
		this.tweets = new ArrayList<Tweet>();
	}
	
	public void addTweet(Tweet t) {
		tweets.add(t);
	}
	
	public List<Tweet> getTweets(int from, int to) {
		return Util.checkedSubList(tweets, from, to);
	}
}
