package hw.demo;

import sapphire.app.SapphireObject;

public class finance implements SapphireObject {
    private wallet wallet;
    private bankaccount bankaccount;

    public finance(wallet wallet, bankaccount bankaccount) {
        this.wallet = wallet;
        this.bankaccount = bankaccount;
    }

    public void transferFromWallet(int amount) {
        this.wallet.debit(amount);
        this.bankaccount.credit(amount);
    }
}
