package sapphire.policy.scalability;

import java.sql.Timestamp;

/**
 * A lease lock that will expire after {@link #lockTimeoutInMillis} milliseconds.
 *
 * Lock is a tuple of (clientId, logIndex, lastUpdatedTimestamp) in which clientId the Id of
 * the client who owns the lock, logIndex is the largest append logIndex reported by the client,
 * and lastUpdatedTimestamp is the timestamp when the lock was updated.
 *
 * {@link #logIndex logIndex} and {@link #lastUpdatedTimestamp lastUpdatedTimestamp} will never
 * decrease.
 *
 * @author terryz
 */
public class Lock {
    private String clientId;
    private long logIndex;
    private Timestamp lastUpdatedTimestamp;
    private final long lockTimeoutInMillis;

    public Lock(String clientId, long logIndex, long lockTimeoutInMillis) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId not specified");
        }

        if (logIndex < 0) {
            throw new IllegalArgumentException(String.format("invalid negative append append index(%s)", logIndex));
        }

        if (lockTimeoutInMillis <= 0) {
            throw new IllegalArgumentException(String.format("invalid lockTimeoutInMillis(%s)", lockTimeoutInMillis));
        }

        this.clientId = clientId;
        this.logIndex = logIndex;
        this.lastUpdatedTimestamp = new Timestamp(System.currentTimeMillis());
        this.lockTimeoutInMillis = lockTimeoutInMillis;
    }

    public String getClientId() {
        return clientId;
    }

    public long getLogIndex() {
        return logIndex;
    }

    public boolean isExpired() {
        Timestamp lockExpirationTime = new Timestamp(lastUpdatedTimestamp.getTime() + lockTimeoutInMillis);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.after(lockExpirationTime);
    }

    public Lock setClientId(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId not specified");
        }

        this.clientId = clientId;
        return this;
    }

    public Lock setLogIndex(long logIndex) {
        if (logIndex < 0) {
            throw new IllegalArgumentException(String.format("invalid negative append index(%s)", logIndex));
        }

        if (logIndex < this.logIndex) {
            throw new IllegalArgumentException(String.format("invalid append index(%s) because it is less than the current append index(%s)", logIndex, this.logIndex));
        }

        this.logIndex = logIndex;
        return this;
    }

    public Lock setLastUpdatedTimestamp(Timestamp lastUpdatedTimestamp) {
        if (lastUpdatedTimestamp == null) {
            throw new NullPointerException("lastUpdatedTimestamp is null");
        }

        if (lastUpdatedTimestamp.before(this.lastUpdatedTimestamp)) {
            throw new IllegalArgumentException(String.format("new timestamp(%s) is earlier than existing timestamp(%s)", lastUpdatedTimestamp, this.lastUpdatedTimestamp));
        }

        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        return this;
    }

    private Lock updateLastUpdatedTimestamp() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        if (now.before(this.lastUpdatedTimestamp)) {
            throw new IllegalArgumentException(String.format("new timestamp(%s) is earlier than existing timestamp(%s)", lastUpdatedTimestamp, this.lastUpdatedTimestamp));
        }

        this.lastUpdatedTimestamp = now;
        return this;
    }

    /**
     * Lock will be renewed iff 1) the clientId equals the clientId in the lock, 2) client
     * index is greater or equal to the append index in the lock, and 3) the current lock has
     * not expired.
     *
     * @param clientId Id of the client
     * @param logIndex largest append index observed on client
     * @return <code>true</code> if lock renew succeeds; <code>false</code> otherwise
     */
    public boolean renew(String clientId, long logIndex) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId not specified");
        }

        if (clientId.equals(this.clientId) &&  !isExpired() && logIndex >= this.logIndex) {
            this.updateLastUpdatedTimestamp();
            return true;
        }

        return false;
    }

    /**
     * The lock will be granted iff
     * the lock has expired which means the <code>lastUpdatedTimestamp</code> of the current
     * lock has passed the threshold, and the <code>clientIndex</code>clientIndex is greater or
     * equal to the <code>logIndex</code> in the lock.

     * @param clientId
     * @param logIndex
     * @return
     */
    public boolean obtain(String clientId, long logIndex) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId not specified");
        }

        if (this.isExpired() && this.getLogIndex() <= logIndex) {
            this.setClientId(clientId).setLogIndex(logIndex).updateLastUpdatedTimestamp();
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Lock{" +
                "clientId='" + clientId + '\'' +
                ", logIndex=" + logIndex +
                ", lastUpdatedTimestamp=" + lastUpdatedTimestamp +
                ", lockTimeoutInMillis=" + lockTimeoutInMillis +
                '}';
    }
}
