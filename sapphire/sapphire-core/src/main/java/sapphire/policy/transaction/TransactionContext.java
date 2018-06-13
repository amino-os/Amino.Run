package sapphire.policy.transaction;

import sapphire.policy.SapphirePolicy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DCAP transaction context based on thread local storage
 */
public class TransactionContext {
    private static final ThreadLocal<UUID> transactionID = new ThreadLocal<UUID>();
    private static final ThreadLocal<TwoPCParticipants> participants = new ThreadLocal<TwoPCParticipants>();

    // nested level of the current transaction
    private static final ThreadLocal<Integer> transactionEnterCount = new ThreadLocal<Integer>();

    // to keep track of SO objects been processed in the current  transaction primitive op
    private static final ThreadLocal<Set<SapphirePolicy.SapphireClientPolicy>> processedClients = new ThreadLocal<Set<SapphirePolicy.SapphireClientPolicy>>();

    /**
     * gets the clients of SO objects that haven been processed so far in the current transaction primitive op
     * @return the clients of SO objects been processed
     */
    public static Set<SapphirePolicy.SapphireClientPolicy> getProcessedClients() {
        return processedClients.get();
    }

    /**
     * resets the processed SO clients
     */
    public static void initPrecessed() {
        HashSet<SapphirePolicy.SapphireClientPolicy> emptyClients = new HashSet<SapphirePolicy.SapphireClientPolicy>();
        processedClients.set(emptyClients);
    }

    /**
     * gets the transaction ID of the active transaction if present
     * @return the current transaction ID or null if being absent
     */
    public static UUID getCurrentTransaction() { return transactionID.get(); }

    /**
     * gets the participants of the active transaction
     * @return TwoPCParticipants object if transaction is present, null otherwise
     */
    public static TwoPCParticipants getParticipants() {
        return participants.get();
    }

    /**
     * enters the transaction
     * @param transactionId ID of the transaction this context is about to enter
     * @param participantManager the participants manager object that manages transaction participants
     */
    public static void enterTransaction(UUID transactionId, TwoPCParticipants participantManager) {
        if (transactionId.equals(getCurrentTransaction())){
            transactionEnterCount.set(transactionEnterCount.get() + 1);
        }else{
            transactionID.set(transactionId);
            participants.set(participantManager);
            transactionEnterCount.set(1);
            initPrecessed();
        }
    }

    /**
     * leaves the transaction after clean up the transaction related data
     */
    public static void leaveTransaction() {
        if (transactionEnterCount.get() != null) {
            transactionEnterCount.set(transactionEnterCount.get() - 1);
            if (transactionEnterCount.get() == 0) {
                transactionID.remove();
                participants.remove();
            }
        }
    }
}
