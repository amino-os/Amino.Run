package amino.run.kernel.server;

import amino.run.common.AppObjectStub;
import amino.run.common.ObjectHandler;
import amino.run.common.Utils;
import amino.run.kernel.common.KernelObjectMigratingException;
import amino.run.kernel.metric.RPCMetric;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * A single MicroService kernel object that can receive RPCs. These are stored in the MicroService
 * kernel server.
 *
 * @author iyzhang
 */
public class KernelObject extends ObjectHandler {

    private static final int MAX_CONCURRENT_RPCS = 100;
    private Boolean coalesced;
    private Semaphore rpcCounter;

    /* Map of RPC metric maintained for each Caller Id i.e., for each client */
    private transient ConcurrentHashMap<UUID, RPCMetric> metrics;

    private static Logger logger = Logger.getLogger(KernelObject.class.getName());

    public KernelObject(Object obj) {
        super(obj);
        coalesced = false;
        rpcCounter = new Semaphore(MAX_CONCURRENT_RPCS, true);
        metrics = new ConcurrentHashMap<UUID, RPCMetric>();
    }

    public ConcurrentHashMap<UUID, RPCMetric> getMetrics() {
        return metrics;
    }

    public Object invoke(AppObjectStub.Context context, String method, ArrayList<Object> params)
            throws Exception {
        Object ret = null;

        if (coalesced) {
            // Object has been migrated to the other kernel server.
            throw new KernelObjectMigratingException(
                    "Object in this kernel server was migrated and is no longer valid.");
        }

        rpcCounter.acquire();

        long startTime = System.nanoTime();

        // Added try finally so that, when the super.invoke(...) throws exceptions,
        // then we safely release the rpcCounter
        try {
            ret = super.invoke(method, params);
        } finally {
            // TODO: Need to reconsider exception case
            long endTime = System.nanoTime();
            recordRPCMetric(context, params, ret, startTime, endTime);
            rpcCounter.release();
        }

        return ret;
    }

    /**
     * Compute and record RPC metrics for the given context
     *
     * @param context Client context received in {@link amino.run.kernel.common.KernelRPC#context}
     * @param input Input to RPC invocation as received in {@link
     *     amino.run.kernel.common.KernelRPC#params}
     * @param output Output of RPC invocation
     * @param startTime Time recorded before calling {@link
     *     amino.run.kernel.server.KernelObject#invoke(String, ArrayList)}
     * @param endTime Time recorded after returning {@link
     *     amino.run.kernel.server.KernelObject#invoke(String, ArrayList)}
     */
    private void recordRPCMetric(
            AppObjectStub.Context context,
            ArrayList<Object> input,
            Object output,
            long startTime,
            long endTime) {
        if (context == null) {
            /* Not an onRPC call. Just return */
            return;
        }

        // Measure the data in, out and rpc elapsed time are record them in metrics map */
        RPCMetric rpcMetric = metrics.get(context.callerId);
        if (rpcMetric == null) {
            rpcMetric = new RPCMetric(context.callerId, context.host);
            metrics.put(context.callerId, rpcMetric);
        }

        /* Record elapsed time */
        rpcMetric.elapsedTime += (endTime - startTime);

        try {
            /* Record data in and out size in bytes */
            rpcMetric.dataSize += Utils.toBytes(input).length;
            rpcMetric.dataSize += Utils.toBytes(output).length;
        } catch (IOException e) {
            logger.severe(
                    String.format(
                            "Failed to calculate the input/output data length. Exception : %s", e));
        }
    }

    public void coalesce() {
        coalesced = true;
        while (rpcCounter.availablePermits() < MAX_CONCURRENT_RPCS - 1) {
            try {
                Thread.sleep(10); // TODO: Quinton: Why sleep for an arbitrary amount of time here?
                // Seems like we need to block on a semaphore here instead.
            } catch (InterruptedException e) {
                continue;
            }
        }

        return;
    }

    public void uncoalesce() {
        coalesced = false;
        // reset the rpc semaphore
        rpcCounter = new Semaphore(MAX_CONCURRENT_RPCS, true);
    }
}
