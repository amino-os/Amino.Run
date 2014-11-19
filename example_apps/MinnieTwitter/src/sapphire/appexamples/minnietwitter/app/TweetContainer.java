package sapphire.appexamples.minnietwitter.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TweetContainer implements Serializable {
	
	private Tweet tweet;
	private List<String> favorited = new ArrayList<String>();
	private List<String> retweeted = new ArrayList<String>();
	
	public TweetContainer(Tweet t, TagManager tm) {
		tweet = t;
		List<Tag> tags = new ArrayList<Tag>();
		
		/* Parse the tweet and get the tags and the mentioned users */
		String text = t.getText();
		
		String[] tokens = text.split(" ");

		for (String word : tokens) {
			if (word.startsWith("#")) {
				tm.addTag(word, t);
				tags.add(new Tag(word));
			}
			//TODO: mentioned users
		}
		t.setTags(tags);
	}

	public void addFavorited(String u) {
		favorited.add(u);
	}

	public void addRetweet(String u) {
		retweeted.add(u);
	}

	public List<String> getFavorited(int from, int to) {
		return Util.checkedSubList(favorited, from, to);
	}

	public List<String> getRetweeted(int from, int to) {
		return Util.checkedSubList(retweeted, from, to);
	}

	public int getFavoritedNumber() {
		return favorited.size();
	}

	public int getRetweetedNumber() {
		return retweeted.size();
	}
	
	public Tweet getTweet() {
		return tweet;
	}
}
