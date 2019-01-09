package amino.run.policy.scalability.masterslave;

/**
 * A lease lock that will expire after {@link #lockTimeoutInMillis} milliseconds.
 *
 * <p>Lock is a tuple of (clientId, lastUpdatedTimestamp) in which clientId the Id of the client who
 * owns the lock and lastUpdatedTimestamp is the timestamp when the lock was updated.
 *
 * @author terryz
 */
public class Lock {
    private String clientId;
    private long lastUpdatedTimestamp;
    private final long lockTimeoutInMillis;

    public Lock(String clientId, long lockTimeoutInMillis) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId not specified");
        }

        if (lockTimeoutInMillis <= 0) {
            throw new IllegalArgumentException(
                    String.format("invalid lockTimeoutInMillis(%s)", lockTimeoutInMillis));
        }

        this.clientId = clientId;
        this.lastUpdatedTimestamp = System.currentTimeMillis();
        this.lockTimeoutInMillis = lockTimeoutInMillis;
    }

    public synchronized String getClientId() {
        return clientId;
    }

    public boolean isExpired() {
        long lockExpirationTime = lastUpdatedTimestamp + lockTimeoutInMillis;
        return System.currentTimeMillis() > lockExpirationTime;
    }

    private Lock updateLastUpdatedTimestamp() {
        this.lastUpdatedTimestamp = System.currentTimeMillis();
        return this;
    }

    /**
     * Lock will be renewed iff 1) the clientId equals the clientId in the lock, and 2) the current
     * lock has not expired.
     *
     * @param clientId Id of the client
     * @return <code>true</code> if lock renew succeeds; <code>false</code> otherwise
     */
    public synchronized boolean renew(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId not specified");
        }

        if (clientId.equals(this.clientId) && !isExpired()) {
            this.updateLastUpdatedTimestamp();
            return true;
        }

        return false;
    }

    /**
     * The lock will be granted iff the lock has expired which means the <code>lastUpdatedTimestamp
     * </code> of the current lock has passed the threshold.
     *
     * @param clientId
     * @return <code>true</code> if the lock is obtained; <code>false</code> otherwise
     */
    public synchronized boolean obtain(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId not specified");
        }

        if (!this.isExpired() && (!clientId.equals(this.clientId))) {
            return false;
        }

        this.clientId = clientId;
        updateLastUpdatedTimestamp();
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Lock{");
        sb.append("clientId='").append(clientId).append('\'');
        sb.append(", lastUpdatedTimestamp=").append(lastUpdatedTimestamp);
        sb.append(", lockTimeoutInMillis=").append(lockTimeoutInMillis);
        sb.append('}');
        return sb.toString();
    }
}
