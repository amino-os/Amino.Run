package hw.demo;

import java.io.Serializable;

public class wallet implements Serializable{
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
