package sapphire.policy.scalability;

import java.util.Random;

/**
 * @author terryz
 */
public class Configuration {
    private final Random random;

    /**
     * MasterLeaseTimeout controls how long the "lease" lasts for
     * being the master. When lease expires, the master will step
     * down and become a slave.
     */
    private final long masterLeaseTimeoutInMillis;

    /**
     *
     */
    private final long masterLeaseRenewIntervalInMillis;

    /**
     *
     */
    private final long shutdownGracePeriodInMillis;

    /**
     *
     */
    private final long initDelayLimitInMillis;

    /**
     *
     */
    private final String logFilePath;

    /**
     *
     */
    private final String snapshotFilePath;

    private Configuration(Builder builder) {
        this.random = new Random(System.currentTimeMillis());
        this.masterLeaseRenewIntervalInMillis = builder.masterLeaseRenewIntervalInMillis;
        this.masterLeaseTimeoutInMillis = builder.masterLeaseTimeoutInMillis;
        this.shutdownGracePeriodInMillis = builder.shutdownGracePeriodInMillis;
        this.initDelayLimitInMillis = builder.initDelayLimitInMillis;
        this.logFilePath = builder.logFilePath;
        this.snapshotFilePath = builder.snapshotFilePath;
    }

    public static final Builder newBuilder() {
        return new Builder();
    }

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

    public String getLogFilePath() {return this.logFilePath;}

    public String getSnapshotFilePath() {return this.snapshotFilePath;}

    public static final class Builder {
        private long masterLeaseTimeoutInMillis = 500;
        private long masterLeaseRenewIntervalInMillis = 100;
        private long shutdownGracePeriodInMillis = 1000;
        private long initDelayLimitInMillis = 500;
        // TODO (Terry) Fix log file path
        private String logFilePath = "/tmp/logFile";
        private String snapshotFilePath = "/tmp/snapshotFile";

        public Builder masterLeaseTimeoutInMIllis(long masterLeaseRenewIntervalInMillis) {
            this.masterLeaseRenewIntervalInMillis = masterLeaseRenewIntervalInMillis;
            return this;
        }

        public Builder masterLeaseRenewIntervalInMillis(long masterLeaseRenewIntervalInMillis) {
            this.masterLeaseRenewIntervalInMillis = masterLeaseRenewIntervalInMillis;
            return this;
        }

        public Builder shutdownGracePeriodInMillis(long shutdownGracePeriodInMillis) {
            this.shutdownGracePeriodInMillis = shutdownGracePeriodInMillis;
            return this;
        }

        public Builder initDelayLimitInMillis(long initDelayLimitInMillis) {
            this.initDelayLimitInMillis = initDelayLimitInMillis;
            return this;
        }

        public Builder logFilePath(String logFilePath) {
            this.logFilePath = logFilePath;
            return this;
        }

        public Builder snapshotFilePath(String snapshotFilePath) {
            this.snapshotFilePath = snapshotFilePath;
            return this;
        }

        public Configuration build() {
            if (masterLeaseRenewIntervalInMillis <= 0) {
                throw new IllegalArgumentException(String.format("negative masterLeaseRenewIntervalInMillis(%s)", masterLeaseRenewIntervalInMillis));
            }

            if (masterLeaseTimeoutInMillis <= 0) {
                throw new IllegalArgumentException(String.format("invalid masterLeaseTimeoutInMillis(%s) ", masterLeaseTimeoutInMillis));
            }

            if (initDelayLimitInMillis <= 0) {
                throw new IllegalArgumentException(String.format("invalid initDelayLimitInMillis(%s)", initDelayLimitInMillis));
            }

            if (masterLeaseTimeoutInMillis <= masterLeaseRenewIntervalInMillis) {
                throw new IllegalArgumentException(String.format("invalid masterLeaseTimeoutInMillis (%s). masterLeaseTimeoutInMillis must be greater than masterLeaseRenewIntervalInMillis (%s).", masterLeaseTimeoutInMillis, masterLeaseRenewIntervalInMillis));
            }

            if (shutdownGracePeriodInMillis <= masterLeaseRenewIntervalInMillis) {
                throw new IllegalArgumentException(String.format("invalid shutdownGracePeriodInMillis (%s). shutdownGracePeriodInMillis must be greater than masterLeaseRenewIntervalInMillis (%s).", shutdownGracePeriodInMillis, masterLeaseRenewIntervalInMillis));
            }

            return new Configuration(this);
        }
    }
}
