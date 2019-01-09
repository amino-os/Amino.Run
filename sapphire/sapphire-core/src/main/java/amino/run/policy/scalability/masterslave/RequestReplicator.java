package amino.run.policy.scalability.masterslave;

import static amino.run.policy.scalability.LoadBalancedMasterSlaveBase.GroupBase;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RequestReplicator replicates requests from master to slaves.
 *
 * @author terryz
 */
public class RequestReplicator implements Replicator, Closeable {
    private static final Logger logger = Logger.getLogger(RequestReplicator.class.getName());
    private final Configuration config;
    private final GroupBase group;
    private volatile ScheduledExecutorService replicator;

    public RequestReplicator(Configuration config, GroupBase group) {
        this.config = config;
        this.group = group;
    }

    @Override
    public void open() {
        this.replicator = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public ReplicationResponse replicateInSync(ReplicationRequest request) {
        ReplicationResponse response = null;
        Future<ReplicationResponse> future = replicateInAsync(request);

        if (future == null) {
            response =
                    new ReplicationResponse(
                            ReplicationResponse.ReturnCode.FAILURE,
                            new Exception("replicator not initialized"));
        } else {
            try {
                response = future.get();
                logger.log(
                        Level.FINER,
                        "successfully replicated request {0}: {1}",
                        new Object[] {request, response});
            } catch (Exception e) {
                logger.log(
                        Level.WARNING,
                        String.format(
                                "failed to replicate request %s: %s", request, e.getMessage()));
            }
        }
        return response;
    }

    @Override
    public Future<ReplicationResponse> replicateInAsync(final ReplicationRequest request) {
        if (replicator == null) {
            logger.log(Level.WARNING, "replicator is not initialized");
            return null;
        }

        return replicator.submit(
                new Callable<ReplicationResponse>() {
                    @Override
                    public ReplicationResponse call() throws Exception {
                        ReplicationResponse response = null;
                        try {
                            response = group.getSlave().handleReplication(request);
                            logger.log(
                                    Level.FINER,
                                    "successfully replicated request {0}: {1}",
                                    new Object[] {request, response});
                        } catch (Exception e) {
                            logger.log(
                                    Level.WARNING,
                                    String.format(
                                            "failed to replicate request %s: %s",
                                            request, e.getMessage()));
                        }
                        return response;
                    }
                });
    }

    @Override
    public void close() throws IOException {
        if (replicator != null) {
            replicator.shutdown();
            try {
                if (!replicator.awaitTermination(
                        config.getShutdownGracePeriodInMillis(), TimeUnit.MILLISECONDS)) {
                    logger.log(
                            Level.SEVERE,
                            "replicator shut down time out after {0} milliseconds",
                            config.getShutdownGracePeriodInMillis());
                }
            } catch (Exception e) {
                logger.log(
                        Level.SEVERE,
                        String.format("got exception during replicator shut down: %s", e),
                        e);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
