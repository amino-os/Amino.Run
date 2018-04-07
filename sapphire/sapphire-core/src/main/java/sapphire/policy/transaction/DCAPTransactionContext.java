package sapphire.policy.transaction;

import java.util.UUID;

public class DCAPTransactionContext {
    private static final ThreadLocal<UUID> transactionID = new ThreadLocal<UUID>();
    private static final ThreadLocal<I2PCParticipants> paricipants = new ThreadLocal<I2PCParticipants>();

    public static UUID getCurrentTransaction() { return transactionID.get(); }

    public static I2PCParticipants getParticipants() {
        return paricipants.get();
    }

    public static void enter(UUID transactionId) {
        transactionID.set(transactionId);
    }

    public static void leave() {
        transactionID.remove();
        paricipants.remove();
    }
}
