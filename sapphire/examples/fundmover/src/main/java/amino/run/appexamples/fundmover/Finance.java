package amino.run.appexamples.fundmover;

import amino.run.app.SapphireObject;


public class Finance implements SapphireObject {
    private Wallet wallet;
    private BankAccount bankaccount;

    public Finance(Wallet wallet, BankAccount bankaccount) {
        this.wallet = wallet;
        this.bankaccount = bankaccount;
    }

    public void transferFromWallet(int amount) throws Exception {
        this.bankaccount.credit(amount);
        this.wallet.debit(amount);

    }
    public void transferFromBank(int amount) throws Exception {
        this.bankaccount.debit(amount);
        this.wallet.credit(amount);

    }

    public String getDetails() {
        return String.format("wallet balance = %d, bank balance = %d", this.wallet.getBalance(), this.bankaccount.getBalance());
    }
}
