package sapphire.policy.scalability;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author terryz
 */
public class AsyncReplicator implements IReplicator, Closeable {
    private final static Logger logger = Logger.getLogger(AsyncReplicator.class.getName());
    private final ScheduledExecutorService replicator;
    private final Configuration config;

    public AsyncReplicator(LoadBalancedMasterSlavePolicy.GroupPolicy group, ILogger<LogEntry> entryLogger, Configuration config) {
        if (group == null) {
            throw new IllegalArgumentException("group policy not specified");
        }

        this.config = config;
        this.replicator = Executors.newSingleThreadScheduledExecutor();
        this.replicator.schedule(new DoReplication(group, entryLogger),
                config.getReplicationIntervalInMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        if (replicator != null) {
            replicator.shutdown();
            try {
                if (! replicator.awaitTermination(config.getShutdownGracePeriodInMillis(), TimeUnit.MILLISECONDS)) {
                    logger.log(Level.SEVERE, "replicator shut down time out after {0} milliseconds", config.getShutdownGracePeriodInMillis());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "got exception during replicator shut down {0}", e);
            }
        }
    }

    private static class DoReplication implements Callable<ReplicationResponse> {
        final LoadBalancedMasterSlavePolicy.GroupPolicy group;
        final ILogger<LogEntry> entryLogger;

        public DoReplication(LoadBalancedMasterSlavePolicy.GroupPolicy group, ILogger<LogEntry> entryLogger) {
            this.group = group;
            this.entryLogger = entryLogger;
        }

        @Override
        public ReplicationResponse call() throws Exception {
            long indexOfLargestReplicatedEntry = entryLogger.getIndexOfLargestReplicatedEntry();
            List<LogEntry> unreplicatedEntries = entryLogger.getUnreplicatedEntries();

            ReplicationRequest request = ReplicationRequest.newBuilder()
                    .indexOfPreviousSyncedEntry(indexOfLargestReplicatedEntry)
                    .entries(unreplicatedEntries)
                    .build();

            LoadBalancedMasterSlavePolicy.ServerPolicy slave = group.getSlave();
            ReplicationResponse response = slave.handleReplication(request);
            switch (response.getReturnCode()) {
                case SUCCESS:
                    Long largestSucceededIndex = (Long)response.getResult();
                    entryLogger.markReplicated(largestSucceededIndex);
                    break;
                case FAILURE:
                    logger.log(Level.SEVERE, "failed to replicate request {0}: {1}", new Object[]{request, response});
                    break;
                case TRACEBACK:
                    throw new AssertionError("trace back should not happen");
                default:
                    throw new AssertionError(String.format("invalid return code in response %s", response));
            }
            return response;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
