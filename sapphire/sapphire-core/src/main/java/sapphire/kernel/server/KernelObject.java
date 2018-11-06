package sapphire.kernel.server;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import sapphire.common.ObjectHandler;
import sapphire.kernel.common.KernelObjectMigratingException;

/**
 * A single Sapphire kernel object that can receive RPCs. These are stored in the Sapphire kernel
 * server.
 *
 * @author iyzhang
 */
public class KernelObject extends ObjectHandler {

    private static final int MAX_CONCURRENT_RPCS = 100;
    private Boolean coalesced;
    private Semaphore rpcCounter;
    private static Logger logger = Logger.getLogger(KernelObject.class.getName());

    public KernelObject(Object obj) {
        super(obj);
        coalesced = false;
        rpcCounter = new Semaphore(MAX_CONCURRENT_RPCS, true);
    }

    public Object invoke(String method, ArrayList<Object> params) throws Exception {
        Object ret;

        if (coalesced) {
            // Object has been migrated to the other kernel server.
            throw new KernelObjectMigratingException(
                    "Object in this kernel server was migrated and is no longer valid.");
        }

        rpcCounter.acquire();

        // Added try finally so that, when the super.invoke(...) throws exceptions,
        // then we safely release the rpcCounter
        try {
            ret = super.invoke(method, params);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Invocation failed for " + method, e);
            // Throwing the exception again so that the same exception is returned to the client.
            // The client is expected to take appropriate action based on this exception.
            throw e;
        } finally {
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
