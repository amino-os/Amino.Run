package sapphire.appexamples.minnietwitter.app;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import sapphire.app.AppObjectNotCreatedException;
import sapphire.app.SapphireObject;
import static sapphire.runtime.Sapphire.*;
import sapphire.policy.dht.DHTPolicy;
import sapphire.policy.interfaces.dht.DHTInterface;
import sapphire.policy.interfaces.dht.DHTKey;

public class UserManager implements SapphireObject<DHTPolicy>, DHTInterface {
	Map<DHTKey, User> users;
	private TagManager tm;

	public UserManager(TagManager tm) {
		this.tm = tm;
		this.users = new Hashtable<DHTKey, User>();
	}

	public User addUser(String username, String passwd) throws AppObjectNotCreatedException {

		User user = (User) new_(User.class, new UserInfo(username, passwd), tm);
		user.initialize(user);
		users.put(new DHTKey(username), user);

		System.out.println("Created user: " + username);
		return user;
	}

	public User getUser(String username) {
		// TODO: check
		return users.get(new DHTKey(username));
	}

	// TODO: throws exception; test
	public User login(String username, String passwd) {
		User u = users.get(new DHTKey(username));

		if (u == null)
			return null;

		// check password
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] pass = md.digest(passwd.getBytes("UTF-8"));
			if (!checkPasswords(pass, u.getUserInfo().getPassword()))
				return null;
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: throw exception
		}
		return u;
	}

	private boolean checkPasswords(byte[] p1, byte[] p2) {
		if (p1.length != p2.length)
			return false;

		for (int i = 0; i < p1.length; i++) {
			int loperand = (p1[i] & 0xff);
			int roperand = (p2[i] & 0xff);
			if (loperand != roperand) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Map<DHTKey, ?> dhtGetData() {
		return users;
	}
}
