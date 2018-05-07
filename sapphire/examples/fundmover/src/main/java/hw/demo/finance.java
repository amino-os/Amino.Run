package hw.demo;

import sapphire.app.SapphireObject;
import sapphire.policy.transaction.TwoPCCoordinatorPolicy;

public class finance implements SapphireObject<TwoPCCoordinatorPolicy> {
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

    public String getDetails() {
        return String.format("wallet balance = %d, bank balance = %d", this.wallet.getBalance(), this.bankaccount.getBalance());
    }
}
