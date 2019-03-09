package amino.run.policy.scalability.masterslave;

import static amino.run.policy.scalability.masterslave.MethodInvocationResponse.ReturnCode.FAILURE;
import static amino.run.policy.scalability.masterslave.MethodInvocationResponse.ReturnCode.SUCCESS;

import amino.run.common.AppObject;
import amino.run.policy.scalability.LoadBalancedMasterSlaveBase;
import amino.run.runtime.exception.AminoRunException;
import amino.run.runtime.exception.AppExecutionException;
import java.io.Closeable;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Committer applies method invocation requests onto App objects. It synchronizes all reads and
 * writes to the App object.
 *
 * @author terryz
 */
public class Committer implements Closeable {
    private final Logger logger = Logger.getLogger(Committer.class.getName());

    /**
     * Index of the largest committed log entry. A log entry is committed iff its request has been
     * invoked on appObject.
     */
    private volatile long indexOfLargestCommittedEntry;

    private final AppObject appObject;
    private final Configuration config;
    private ExecutorService executor;

    // TODO (Terry): remove indexOfLargestCommittedEntry from constructor
    public Committer(AppObject appObject, long indexOfLargestCommittedEntry, Configuration config) {
        this.appObject = appObject;
        this.indexOfLargestCommittedEntry = indexOfLargestCommittedEntry;
        this.config = config;
    }

    /** Initializes Committer. */
    public void open() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(
                        config.getShutdownGracePeriodInMillis(), TimeUnit.MILLISECONDS)) {
                    // shutdown time out
                    logger.log(
                            Level.SEVERE,
                            "Committer shut down time out after {0} milliseconds",
                            config.getShutdownGracePeriodInMillis());
                }
            } catch (InterruptedException e) {
                logger.log(
                        Level.SEVERE,
                        String.format("got exception during Committer shut down: %s", e),
                        e);
            }
        }
    }

    /**
     * Invokes read operation on <code>appObject</code>.
     *
     * @param request
     * @return
     */
    public MethodInvocationResponse applyRead(final MethodInvocationRequest request) {
        if (executor == null) {
            logger.log(Level.WARNING, "executor is not initialized");
            return new MethodInvocationResponse(FAILURE, new Exception("executor not initialized"));
        }

        Future<Object> future =
                executor.submit(
                        new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                synchronized (appObject) {
                                    try {
                                        Object result =
                                                appObject.invoke(
                                                        request.getMethodName(),
                                                        request.getParams());
                                        logger.log(
                                                Level.FINER,
                                                "result for request {0}: {1}",
                                                new Object[] {request, result});
                                        return result;
                                    } catch (Exception e) {
                                        logger.log(
                                                Level.SEVERE,
                                                String.format(
                                                        "failed to invoke write request %s on app object: "
                                                                + "%s",
                                                        request, e),
                                                e);
                                        throw e;
                                    }
                                }
                            }
                        });

        return createResponse(request, future);
    }

    /**
     * Invokes write operation on <code>appObject</code> synchronously.
     *
     * @param request
     * @param entryIndex
     * @return
     */
    public MethodInvocationResponse applyWriteSync(
            final MethodInvocationRequest request, final long entryIndex) {
        Future<Object> future = applyWriteAsync(request, entryIndex);
        return createResponse(request, future);
    }

    /**
     * Invoke write operation on <code>appObject</code> asynchronously.
     *
     * <p>Entry indices have to increase monotonically
     *
     * @param request
     * @param entryIndex
     * @return
     */
    public Future<Object> applyWriteAsync(
            final MethodInvocationRequest request, final long entryIndex) {
        if (executor == null) {
            logger.log(Level.WARNING, "executor is not initialized");
            return null;
        }

        return executor.submit(
                new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        if (entryIndex <= getIndexOfLargestCommittedEntry()) {
                            String msg =
                                    String.format(
                                            "forbidden to commit old entry %s "
                                                    + "against the current largest committed entry %s",
                                            entryIndex, getIndexOfLargestCommittedEntry());
                            logger.log(Level.SEVERE, msg);
                            throw new IllegalStateException(msg);
                        } else {
                            synchronized (appObject) {
                                try {
                                    Object result =
                                            appObject.invoke(
                                                    request.getMethodName(), request.getParams());
                                    markCommitted(entryIndex);
                                    logger.log(
                                            Level.FINER,
                                            "result for request {0}: {1}",
                                            new Object[] {request, result});
                                    return result;
                                } catch (Exception e) {
                                    logger.log(
                                            Level.SEVERE,
                                            String.format(
                                                    "failed to invoke write request %s on app object: "
                                                            + "%s",
                                                    request, e),
                                            e);
                                    throw e;
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Update App object and the committed index
     *
     * @param object the new value of the App object. After this method call, the App object managed
     *     by this committer will be replaced with this object.
     * @param largestCommittedIndex
     */
    public void updateObject(Serializable object, long largestCommittedIndex) {
        synchronized (appObject) {
            appObject.setObject(object);
            markCommitted(largestCommittedIndex);
        }
    }

    /**
     * Replicates App object to the given destination server
     *
     * @param server destination server
     */
    public void syncObject(LoadBalancedMasterSlaveBase.ServerPolicy server) {
        synchronized (appObject) {
            server.syncObject(appObject.getObject(), getIndexOfLargestCommittedEntry());
        }
    }

    /** @return the index of the largest committed entry */
    public long getIndexOfLargestCommittedEntry() {
        return indexOfLargestCommittedEntry;
    }

    /**
     * Sets the index of the largest committed entry
     *
     * @param indexOfLargestCommittedEntry
     */
    public void setIndexOfLargestCommittedEntry(long indexOfLargestCommittedEntry) {
        this.indexOfLargestCommittedEntry = indexOfLargestCommittedEntry;
    }

    /**
     * Marks the given log entry as applied. A log entry is applied iff its request has been invoked
     * on slave.
     *
     * <p>On mater, an entry is committed means the request in the entry has been invoked but the
     * entry may not be replicated to slave. On slave, an entry is committed means the request has
     * been invoked on slave.
     *
     * @param index
     */
    private void markCommitted(long index) {
        if (index <= indexOfLargestCommittedEntry) {
            throw new IllegalStateException(
                    String.format(
                            "failed to mark log entry with index %s "
                                    + "as of committed log entry because its index is less than indexOfLargestCommittedEntry %s",
                            index, getIndexOfLargestCommittedEntry()));
        }

        setIndexOfLargestCommittedEntry(index);
    }

    private MethodInvocationResponse createResponse(
            MethodInvocationRequest request, Future future) {
        MethodInvocationResponse response;

        if (future == null) {
            response =
                    new MethodInvocationResponse(
                            FAILURE, new Exception("executor not initialized"));
        } else {
            try {
                Object result = future.get();
                response = new MethodInvocationResponse(SUCCESS, result);
                logger.log(
                        Level.FINER,
                        "response for request {0}: {1}",
                        new Object[] {request, response});
            } catch (ExecutionException e) {
                // Method invocation on application object failed.
                // This is caused by application errors, not MicroService errors
                AppExecutionException ex =
                        new AppExecutionException("method invocation on app object failed", e);
                logger.log(
                        Level.FINE,
                        String.format(
                                "failed to process request %s on %s: %s", request, appObject, ex),
                        ex);
                response = new MethodInvocationResponse(FAILURE, ex);
            } catch (Exception e) {
                // This is likely caused by MicroService errors. We treat it as an application error
                // for
                // now.
                // The problem is: the other replica may not run into this exception. This exception
                // may cause two replicas out of sync.
                AminoRunException ex =
                        new AminoRunException("Method invocation on app object was interrupted", e);
                logger.log(
                        Level.SEVERE,
                        String.format(
                                "the process of request %s on %s was interrupted: %s",
                                request, appObject, ex),
                        e);
                response = new MethodInvocationResponse(FAILURE, ex);
            }
        }
        return response;
    }
}
