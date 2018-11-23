package sapphire.appexamples.fundmover;

import sapphire.app.SapphireObject;
import sapphire.policy.serializability.TransactionAlreadyStartedException;
import sapphire.policy.transaction.TransactionExecutionException;
import sapphire.policy.transaction.TransactionManager;
import java.util.UUID;

public class BankAccount implements SapphireObject, TransactionManager {
    private int balance;
    public BankAccount() {
    }

    public void credit(int amount) {
        this.balance += amount;
    }

    public void debit(int amount) throws Exception{
        if (this.balance < amount) {
            throw new Exception("insufficient fund");
        }
        this.balance -= amount;
    }

    public int getBalance() {
        return this.balance;
    }

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {
        System.out.println("[bank] xa join");

    }

    @Override
    public void leave(UUID transactionId) {
        System.out.println("[bank] xa leave");
    }

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        System.out.println("[bank] xa vote");
        return Vote.YES;
    }

    @Override
    public void commit(UUID transactionId) {
        System.out.println("[bank] xa commit");
    }

    @Override
    public void abort(UUID transactionId) {
        System.out.println("[bank] xa abort");
    }


}
