package hw.demo;

public class wallet {
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
