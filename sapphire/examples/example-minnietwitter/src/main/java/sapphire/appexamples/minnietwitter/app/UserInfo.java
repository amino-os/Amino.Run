package sapphire.appexamples.minnietwitter.app;

import java.io.Serializable;
import java.security.MessageDigest;

public class UserInfo implements Serializable {
	/* Unique username */
	String username;
	/* MDH5 of password*/
	byte[] password;
	
	public UserInfo(String username, String passwd) {
		this.username = username;
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			this.password = md.digest(passwd.getBytes("UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public byte[] getPassword() {
		return password;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}
}
