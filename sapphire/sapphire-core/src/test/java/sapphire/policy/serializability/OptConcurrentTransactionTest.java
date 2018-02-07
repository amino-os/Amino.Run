package sapphire.policy.serializability;

/**
 * Created by root1 on 7/2/18.
 */

public class OptConcurrentTransactionTest extends OptConcurrentTransactionImpl {
    int i = 0;
    public void setI(int i) {
        this.i = i;
    }
    public int getI() {
        return i;
    }
}