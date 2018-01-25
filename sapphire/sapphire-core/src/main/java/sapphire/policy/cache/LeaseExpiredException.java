package sapphire.policy.cache;

/**
 * Created by quinton on 1/24/18.
 */

public class LeaseExpiredException extends Exception {
    public LeaseExpiredException(String s) {
        super(s);
    }
}
