package sapphire.policy.scalability;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author terryz
 */
public class AsyncReplicator implements IReplicator, Closeable {
    private final static Logger logger = Logger.getLogger(AsyncReplicator.class.getName());
    private final ScheduledExecutorService replicationExecutor;

    public AsyncReplicator(LoadBalancedMasterSlavePolicy.ServerPolicy slave, ILogger<LogEntry> entryLogger) {
        if (slave == null) {
            throw new IllegalArgumentException("no slave server available");
        }

        this.replicationExecutor = Executors.newSingleThreadScheduledExecutor();
        this.replicationExecutor.schedule(new DoReplication(slave, entryLogger),
                1000, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws IOException {
        if (replicationExecutor != null) {
            replicationExecutor.shutdown();
        }
    }

    private static class DoReplication implements Callable<ReplicationResponse> {
        final LoadBalancedMasterSlavePolicy.ServerPolicy slave;
        final ILogger<LogEntry> entryLogger;

        public DoReplication(LoadBalancedMasterSlavePolicy.ServerPolicy slave, ILogger<LogEntry> entryLogger) {
            this.slave = slave;
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
