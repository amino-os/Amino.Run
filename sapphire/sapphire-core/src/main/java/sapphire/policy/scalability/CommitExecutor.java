package sapphire.policy.scalability;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.AppObject;
import sapphire.runtime.MethodInvocationRequest;
import sapphire.runtime.MethodInvocationResponse;
import sapphire.runtime.exception.AppExecutionException;

import static sapphire.runtime.MethodInvocationResponse.ReturnCode.FAILURE;
import static sapphire.runtime.MethodInvocationResponse.ReturnCode.SUCCESS;

/**
 *
 * @author terryz
 */
public class CommitExecutor implements Closeable {
    /**
     * Index of the largest committed log entry. A log entry is
     * committed iff its request has been invoked on appObject.
     */
    private final AtomicLong indexOfLargestCommittedEntry;

    private final Logger logger = Logger.getLogger(CommitExecutor.class.getName());
    private final ExecutorService executor;
    private final AppObject appObject;

    public CommitExecutor(AppObject appObject) {
        this(appObject, 0L);
    }

    public CommitExecutor(AppObject appObject, long indexOfLargestCommittedEntry) {
        this.appObject = appObject;
        this.indexOfLargestCommittedEntry = new AtomicLong(indexOfLargestCommittedEntry);
        this.executor =  Executors.newSingleThreadExecutor();
    }

    /**
     * Invokes read operation on <code>appObject</code>.
     *
     * @param request
     * @return
     */
    public MethodInvocationResponse applyRead(final MethodInvocationRequest request) {
        Future<Object> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                synchronized (appObject) {
                    return appObject.invoke(request.getMethodName(), request.getParams());
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
    public MethodInvocationResponse applyWriteSync(final MethodInvocationRequest request, final long entryIndex) {
        Future<Object> future = applyWriteAsync(request, entryIndex);
        return createResponse(request, future);
    }

    /**
     * Invoke write operation on <code>appObject</code> asynchronously.
     *
     * @param request
     * @param entryIndex
     * @return
     */
    public Future<Object> applyWriteAsync(final MethodInvocationRequest request, final long entryIndex) {
        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // invokeMethod and markCommitted should be protected by a lock.
                // The system should grab the same lock before taking snapshot.
                synchronized (appObject) {
                    Object result = appObject.invoke(request.getMethodName(), request.getParams());
                    markCommitted(entryIndex);
                    return result;
                }
            }
        });
    }

    /**
     *
     * @param entry
     * @return
     * @throws Exception
     */
    public synchronized SnapshotEntry updateSnapshot(SnapshotEntry entry) throws Exception {
        synchronized (appObject) {
            return SnapshotEntry.newBuilder()
                    .term(entry.getTerm())
                    .index(entry.getIndex())
                    .logFilePath(entry.getLogFilePath())
                    .snapshotFilePath(entry.getSnapshotFilePath())
                    .appObject(appObject)
                    .lowestOffsetInLogFile(entry.getLowestOffsetInLogFile())
                    .indexOfLargestCommittedEntry(indexOfLargestCommittedEntry.get())
                    .indexOfLargestReplicatedEntry(entry.getIndexOfLargestReplicatedEntry())
                    .build();
        }
    }

    public long getIndexOfLargestCommittedEntry() {
        return indexOfLargestCommittedEntry.get();
    }

    public void setIndexOfLargestCommittedEntry(long indexOfLargestCommittedEntry) {
        this.indexOfLargestCommittedEntry.set(indexOfLargestCommittedEntry);
    }

    /**
     * Marks the given log entry as applied. A log entry is applied iff
     * its request has been invoked on slave.
     * <p>
     *
     * On mater, an entry is committed means the request in the entry has
     * been invoked but the entry may not be replicated to slave. On slave,
     * an entry is committed means the request has been invoked on slave.
     *
     * @param index
     */
    private void markCommitted(long index) {
        if (index <= indexOfLargestCommittedEntry.get()) {
            throw new IllegalStateException(String.format("failed to mark log entry with index %s " +
                    "as of committed log entry because its index is less than indexOfLargestCommittedEntry %s",
                    index, getIndexOfLargestCommittedEntry()));
        }

        this.indexOfLargestCommittedEntry.set(index);
    }

    private MethodInvocationResponse createResponse(MethodInvocationRequest request, Future future) {
        MethodInvocationResponse resp;
        try {
            Object ret = future.get();
            resp = MethodInvocationResponse.newBuilder().returnCode(SUCCESS).result(ret).build();
        } catch (ExecutionException e) {
            // Method invocation on application object failed.
            // This is caused by application errors, not Sapphire errors
            AppExecutionException ex = new AppExecutionException("method invocation on app object failed", e);
            logger.log(Level.FINE, "failed to process request {0} on {1}: {2}", new Object[]{request, appObject, ex});
            resp = MethodInvocationResponse.newBuilder().returnCode(FAILURE).result(ex).build();
        } catch (InterruptedException e) {
            // TODO (Terry): what should we do here?
            // This is likely caused by Sapphire errors.
            // The problem is: the other replica may not run into this exception. This exception
            // may cause two replicas out of sync.
            AppExecutionException ex = new AppExecutionException("Method invocation on appobject was interrupted", e);
            logger.log(Level.SEVERE, "the process of request {0} on {1} was interrupted: {2}", new Object[]{request, appObject, ex});
            resp = MethodInvocationResponse.newBuilder().returnCode(FAILURE).result(ex).build();
        }

        return resp;
    }

    @Override
    public void close() throws IOException {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
