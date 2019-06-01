package amino.run.appexamples.minnietwitter;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserInfo implements Serializable {
    /* Unique username */
    String username;
    /* MDH5 of password*/
    byte[] password;

    public UserInfo(String username, String passwd)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        this.username = username;
        MessageDigest md;
        md = MessageDigest.getInstance("MD5");
        this.password = md.digest(passwd.getBytes("UTF-8"));
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
