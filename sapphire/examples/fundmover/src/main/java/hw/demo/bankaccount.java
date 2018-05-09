package hw.demo;

import sapphire.app.SapphireObject;
import sapphire.policy.transaction.TwoPCCohortPolicy;

import java.io.Serializable;

public class bankaccount implements SapphireObject<TwoPCCohortPolicy> {
    private int balance;

    public void credit(int amount) {
        this.balance += amount;
    }

    public void debit(int amount) {
        this.balance -= amount;
    }

    public int getBalance() {
        return this.balance;
    }
}
