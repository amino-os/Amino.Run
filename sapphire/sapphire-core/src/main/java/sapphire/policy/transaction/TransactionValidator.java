package sapphire.policy.transaction;

import java.util.UUID;

/** abstraction of sandbox validation with multiple transactions */
public interface TransactionValidator {
    /**
     * to add the transaction as the promised one (yes-voted)
     *
     * @param transactionId id of the candidate transaction
     * @return true if validated and promised; false otherwise
     */
    public boolean promises(UUID transactionId) throws Exception;

    /**
     * method to be called when this transaction is being committed
     *
     * @param transactionId id of the transaction to commit
     */
    public void onCommit(UUID transactionId);

    /**
     * method to be called when this sandbox is being aborted
     *
     * @param transactionId id of the transaction to abort
     */
    public void onAbort(UUID transactionId);
}
