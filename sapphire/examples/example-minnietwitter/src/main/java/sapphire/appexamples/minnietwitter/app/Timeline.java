package sapphire.appexamples.minnietwitter.app;

import java.util.ArrayList;
import java.util.List;

import sapphire.app.SapphireObject;

public class Timeline implements SapphireObject {
	//private User user;
	private String userName;

	/* TODO: Distributed structure for the tweets? */
	/* TODO: Locking */
	List<TweetContainer> tweets;

	private TagManager tm;
	private int currentId = 0;
	private Timeline timelineStub;

	public Timeline(User user, TagManager tm) {
		//this.user = user;
		this.userName = user.getUserInfo().getUsername();
		this.tm = tm;
		this.tweets = new ArrayList<TweetContainer>();
	}

	public void initialize(Timeline timelineStub) {
		this.timelineStub = timelineStub;
	}

	public void tweet(String text) {
		Tweet t = new Tweet(timelineStub, text, userName, currentId);
		TweetContainer tc = new TweetContainer(t, tm);
		tweets.add(0, tc);
		System.out.println("Tweeted: " + text);
		currentId += 1;
	}

	public void retweet(Tweet t) {
		/* Notify the timeline of the tweet that this tweet has been retweeted */
	    t.getTimeline().retweetedBy(t.getId(), userName);
	    t.setTimeline(timelineStub);
		
	    /* Update this tweet's id on the new timeline */
		t.setId(currentId);
		currentId += 1;
		TweetContainer tc = new TweetContainer(t, tm);

		/* TODO: Extract mentions and hashtags for the retweet? */
		tweets.add(0, tc);
	}

	public void retweetedBy(int tweetId, String userName) {
		TweetContainer tc = getTweetContainer(tweetId);	
		tc.addRetweet(userName);
		tc.getTweet().incRetweetes();
	}

	public void favorite(int tweetId, String userName) {
		TweetContainer tc = getTweetContainer(tweetId);
		tc.addFavorited(userName);
		tc.getTweet().incFavorites();
	}

	public List<Tweet> getTweets(int from, int to) {
		List<TweetContainer> tc = Util.checkedSubList(tweets, from, to);
		List<Tweet> tw = new ArrayList<Tweet>();
		for (TweetContainer c : tc)
			tw.add(c.getTweet());
		return tw;
	}

	public List<String> getRetweets(int tweetId, int from, int to) {
		TweetContainer tc = getTweetContainer(tweetId);
		return tc.getRetweeted(from, to);
	}

	public List<String> getFavorites(int tweetId, int from, int to) {
		TweetContainer tc = getTweetContainer(tweetId);
		return tc.getFavorited(from, to);
	}

	private TweetContainer getTweetContainer(int tweetId) {
		int pos = tweets.size() - 1 - tweetId;
		return tweets.get(pos);
	}
}
