package amino.run.policy.serializability;

/** Created by quinton on 1/22/18. */
public class LockingTransactionTest extends LockingTransactionImpl {
    int i = 0;

    public void setI(int i) {
        this.i = i;
    }

    public int getI() {
        return i;
    }
}
