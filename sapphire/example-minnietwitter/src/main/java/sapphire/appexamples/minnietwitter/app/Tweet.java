package sapphire.appexamples.minnietwitter.app;

import java.io.Serializable;
import java.util.List;

public class Tweet implements Serializable {
	private int id;
	private int favorites = 0;
	private int retweetes = 0;
	private Timeline timeline;
	private String text;
	private List<Tag> tags;
	private String authorUsername;
	
	public Tweet(Timeline timeline, String text, String authorUsername, int id) {
		this.text = text;
	    this.authorUsername = authorUsername;
	    this.timeline = timeline;
	    this.id = id;
	}
	
	public List<Tag> getTags() {
		return tags;
	}
	
	public String getText() {
		return text;
	}

	public String getAuthorName() {
		return authorUsername;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public Timeline getTimeline() {
		return timeline;
	}
	
	public void setTimeline(Timeline timeline) {
		this.timeline = timeline;
	}

	public int getFavorites() {
		return favorites;
	}

	public void incFavorites() {
		favorites += 1;
	}

	public int getRetweetes() {
		return retweetes;
	}

	public void incRetweetes() {
		retweetes += 1;
	}
}
