package hw.demo;

import sapphire.app.SapphireObject;
import sapphire.policy.transaction.TwoPCCohortPolicy;

import java.io.Serializable;

public class wallet implements SapphireObject<TwoPCCohortPolicy> {
    private int balance;

    public void credit(int amount) {
        this.balance += amount;
    }

    public void debit(int amount) throws Exception {
        if (this.balance < amount) {
            throw new Exception("insufficient fund");
        }
        this.balance -= amount;
    }

    public int getBalance() {
        return this.balance;
    }
}
