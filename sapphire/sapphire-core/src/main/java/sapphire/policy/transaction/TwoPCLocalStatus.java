package sapphire.policy.transaction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * type to keep track of transaction's local status
 */
public class TwoPCLocalStatus {
    public enum LocalStatus {
        UNCERTAIN, GOOD, BAD, YESVOTED, NOVOTED, COMMITTED, ABORTED,
    }

    ConcurrentHashMap<UUID, LocalStatus> localStatusManager = new ConcurrentHashMap<>();

    /**
     * set local status of the component in the specified transaction
     * @param id id of the transaction
     * @param status local status
     */
    public void setStatus(UUID id, LocalStatus status) {
        if (status == LocalStatus.COMMITTED || status == LocalStatus.ABORTED) {
            this.localStatusManager.remove(id);
        } else {
            this.localStatusManager.put(id, status);
        }
    }

    /**
     * gets the currrent local status of component in the active transaction
     * @param id id of the transaction
     * @return the local status
     */
    public LocalStatus getStatus(UUID id) {
        return this.localStatusManager.get(id);
    }
}
