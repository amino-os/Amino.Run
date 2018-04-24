package sapphire.policy.transaction;

import java.util.UUID;

/**
 * DCAP transaction context based on thread local storage
 */
public class TransactionContext {
    private static final ThreadLocal<UUID> transactionID = new ThreadLocal<UUID>();
    private static final ThreadLocal<TwoPCParticipants> participants = new ThreadLocal<TwoPCParticipants>();

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
        transactionID.set(transactionId);
        participants.set(participantManager);
    }

    /**
     * leaves the transaction after clean up the transaction related data
     */
    public static void leaveTransaction() {
        transactionID.remove();
        participants.remove();
    }
}
