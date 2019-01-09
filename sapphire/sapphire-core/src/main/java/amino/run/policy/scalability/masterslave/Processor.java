package amino.run.policy.scalability.masterslave;

import static amino.run.policy.scalability.masterslave.MethodInvocationResponse.ReturnCode.FAILURE;

import amino.run.policy.scalability.LoadBalancedMasterSlaveBase;
import amino.run.runtime.exception.AppExecutionException;
import amino.run.runtime.exception.SapphireException;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread safe processor that handles method invocation requests on master.
 *
 * @author terryz
 */
public class Processor implements Closeable {
    private static final Logger logger = Logger.getLogger(Processor.class.getName());
    private final Configuration config;
    private final LoadBalancedMasterSlaveBase.GroupBase group;
    private final Committer commitExecutor;
    private final Replicator replicator;
    private volatile ExecutorService processor;
    // TODO: retire map entries
    private final Map<String, CachedResult> cachedResults;

    public Processor(
            Configuration config,
            LoadBalancedMasterSlaveBase.GroupBase group,
            Committer commitExecutor,
            Replicator replicator) {
        this.config = config;
        this.group = group;
        this.commitExecutor = commitExecutor;
        this.replicator = replicator;
        this.cachedResults = new ConcurrentHashMap<String, CachedResult>();
    }

    /**
     * Initializes the processor. This method must be called before {@link
     * #process(MethodInvocationRequest)} and {@link #processAsync(MethodInvocationRequest)}.
     */
    public void open() {
        this.processor = Executors.newSingleThreadExecutor();
    }

    /**
     * Processes the given method invocation request synchronously
     *
     * @param request method invocation request
     * @return method invocation response
     */
    public MethodInvocationResponse process(MethodInvocationRequest request) {
        final Future<MethodInvocationResponse> future = processAsync(request);
        if (future == null) {
            return new MethodInvocationResponse(
                    FAILURE, new Exception("processor not initialized"));
        }

        try {
            MethodInvocationResponse response = future.get();
            logger.log(
                    Level.FINE, "response for request {0}: {1}", new Object[] {request, response});
            return response;
        } catch (ExecutionException e) {
            logger.log(
                    Level.INFO, String.format("failed to process request %s: %s", request, e), e);
            AppExecutionException ex = new AppExecutionException(e);
            return new MethodInvocationResponse(FAILURE, ex);
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE, String.format("failed to process request %s: %s", request, e), e);
            SapphireException ex = new SapphireException(e);
            return new MethodInvocationResponse(FAILURE, ex);
        }
    }

    /**
     * Processes the given method invocation request asynchronously
     *
     * @param request method invocation request
     * @return a future of the method invocation response
     */
    public Future<MethodInvocationResponse> processAsync(MethodInvocationRequest request) {
        if (processor == null) {
            logger.log(Level.WARNING, "processor is not initialized");
            return null;
        }

        return processor.submit(
                new RequestProcessor(group, commitExecutor, replicator, request, cachedResults));
    }

    private static class RequestProcessor implements Callable<MethodInvocationResponse> {
        private MethodInvocationRequest request;
        private final LoadBalancedMasterSlaveBase.GroupBase group;
        private Committer commitExecutor;
        private Replicator replicator;
        private Map<String, CachedResult> cachedResults;

        public RequestProcessor(
                LoadBalancedMasterSlaveBase.GroupBase group,
                Committer commitExecutor,
                Replicator replicator,
                MethodInvocationRequest request,
                Map<String, CachedResult> cachedResults) {
            this.request = request;
            this.group = group;
            this.commitExecutor = commitExecutor;
            this.replicator = replicator;
            this.cachedResults = cachedResults;
        }

        @Override
        public MethodInvocationResponse call() throws Exception {
            if (request.isImmutable()) {
                return commitExecutor.applyRead(request);
            }

            if (!cachedResults.containsKey(request.getClientId())) {
                cachedResults.put(request.getClientId(), new CachedResult());
            }
            CachedResult cachedResult = cachedResults.get(request.getClientId());

            if (request.getRequestId() < cachedResult.getRequestId()) {
                String msg = String.format("forbidden to rerun old request %s", request);
                logger.log(Level.WARNING, msg);
                throw new Exception(msg);
            } else if (request.getRequestId() == cachedResult.getRequestId()) {
                return cachedResult.getInvocationResponse();
            }

            long largestCommittedIndex = commitExecutor.getIndexOfLargestCommittedEntry();
            LogEntry entry =
                    LogEntry.newBuilder().request(request).index(largestCommittedIndex + 1).build();

            MethodInvocationResponse response =
                    commitExecutor.applyWriteSync(request, entry.getIndex());

            ReplicationRequest replicationRequest =
                    new ReplicationRequest(largestCommittedIndex, Arrays.asList(entry));

            ReplicationResponse rr = replicator.replicateInSync(replicationRequest);
            if (rr != null && rr.getReturnCode() == ReplicationResponse.ReturnCode.TRACEBACK) {
                commitExecutor.syncObject(group.getSlave());
            }

            cachedResult.update(request.getClientId(), request.getRequestId(), response);

            return response;
        }
    }

    @Override
    public void close() throws IOException {
        if (processor != null) {
            processor.shutdown();
            try {
                if (!processor.awaitTermination(
                        config.getShutdownGracePeriodInMillis(), TimeUnit.MILLISECONDS)) {
                    logger.log(
                            Level.SEVERE,
                            "processor shut down time out after {0} milliseconds",
                            config.getShutdownGracePeriodInMillis());
                }
                processor = null;
            } catch (Exception e) {
                logger.log(
                        Level.SEVERE,
                        String.format("got exception during processor shut down: %s", e),
                        e);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private static final class CachedResult implements Serializable {
        private volatile String clientId;
        private volatile long requestId = -1;
        private volatile MethodInvocationResponse invocationResponse;

        public void update(
                String clientId, long requestId, MethodInvocationResponse invocationResponse) {
            this.clientId = clientId;
            this.requestId = requestId;
            this.invocationResponse = invocationResponse;
        }

        public String getClientId() {
            return clientId;
        }

        public long getRequestId() {
            return requestId;
        }

        public MethodInvocationResponse getInvocationResponse() {
            return invocationResponse;
        }
    }
}
