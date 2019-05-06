package amino.run.kernel.server;

import amino.run.common.AppObjectStub;
import amino.run.common.ObjectHandler;
import amino.run.common.Utils;
import amino.run.kernel.common.KernelObjectMigratingException;
import amino.run.kernel.metric.RPCMetric;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

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
    private transient ConcurrentHashMap<UUID, RPCMetric> metrics;

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

        RPCMetric rpcMetric = null;
        long startTime = 0;
        long endTime = 0;
        // Measure the data in, out and rpc processing time
        if (context != null) {
            rpcMetric = metrics.get(context.callerId);
            if (rpcMetric == null) {
                rpcMetric = new RPCMetric(context.host);
                metrics.put(context.callerId, rpcMetric);
                try {
                    rpcMetric.dataSize += Utils.toBytes(params).length;
                } catch (Exception e) {
                }
            }
            startTime = System.nanoTime();
        }

        // Added try finally so that, when the super.invoke(...) throws exceptions,
        // then we safely release the rpcCounter
        try {
            ret = super.invoke(method, params);
        } finally {
            // TODO: Need to reconsider exception case
            if (rpcMetric != null) {
                endTime = System.nanoTime();
                try {
                    rpcMetric.dataSize += Utils.toBytes(ret).length;
                } catch (Exception e) {
                }
                rpcMetric.processTime += (endTime - startTime);
            }
            rpcCounter.release();
        }

        return ret;
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
