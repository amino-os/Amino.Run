package sapphire.policy.scalability.masterslave;

import java.util.Random;

/**
 * @author terryz
 */
public class Configuration {
    private final Random random = new Random(System.currentTimeMillis());

    /**
     * MasterLeaseTimeout controls how long the "lease" lasts for
     * being the master. When lease expires, the master will step
     * down and become a slave.
     */
    private long masterLeaseTimeoutInMillis = 500;

    /**
     * Specifies the frequency for the master to renew the lock
     */
    private long masterLeaseRenewIntervalInMillis = 100;

    /**
     * Specifies the grace period before forcefully terminate executor services
     */
    private long shutdownGracePeriodInMillis = 200;

    /**
     *
     */
    private long initDelayLimitInMillis = 200;

    public long getMasterLeaseTimeoutInMillis() {
        return masterLeaseTimeoutInMillis;
    }

    public long getMasterLeaseRenewIntervalInMillis() {
        return masterLeaseRenewIntervalInMillis;
    }

    public long getShutdownGracePeriodInMillis() {
        return shutdownGracePeriodInMillis;
    }

    public long getInitDelayLimitInMillis() {
        return random.nextLong()%initDelayLimitInMillis;
    }

    public Configuration setMasterLeaseTimeoutInMillis(long masterLeaseTimeoutInMillis) {
        if (masterLeaseTimeoutInMillis <= 0) {
            throw new IllegalArgumentException(String.format("invalid masterLeaseTimeoutInMillis(%s) ", masterLeaseTimeoutInMillis));
        }

        this.masterLeaseTimeoutInMillis = masterLeaseTimeoutInMillis;
        return this;
    }

    public Configuration setMasterLeaseRenewIntervalInMillis(long masterLeaseRenewIntervalInMillis) {
        if (masterLeaseRenewIntervalInMillis <= 0) {
            throw new IllegalArgumentException(String.format("negative masterLeaseRenewIntervalInMillis(%s)", masterLeaseRenewIntervalInMillis));
        }
        this.masterLeaseRenewIntervalInMillis = masterLeaseRenewIntervalInMillis;
        return this;
    }

    public Configuration setShutdownGracePeriodInMillis(long shutdownGracePeriodInMillis) {
        if (shutdownGracePeriodInMillis <= 0) {
            throw new IllegalArgumentException(String.format("negative shutdownGracePeriodInMillis(%s)", shutdownGracePeriodInMillis));
        }

        this.shutdownGracePeriodInMillis = shutdownGracePeriodInMillis;
        return this;
    }

    public Configuration setInitDelayLimitInMillis(long initDelayLimitInMillis) {
        if (initDelayLimitInMillis <= 0) {
            throw new IllegalArgumentException(String.format("invalid initDelayLimitInMillis(%s)", initDelayLimitInMillis));
        }

        this.initDelayLimitInMillis = initDelayLimitInMillis;
        return this;
    }
}
