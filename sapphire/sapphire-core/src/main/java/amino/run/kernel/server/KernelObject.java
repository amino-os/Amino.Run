package amino.run.kernel.server;

import amino.run.common.ObjectHandler;
import amino.run.kernel.common.KernelObjectMigratingException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

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
    public boolean status;
    private static Logger logger = Logger.getLogger(KernelObject.class.getName());

    public KernelObject(Object obj) {
        super(obj);
        coalesced = false;
        status = true;
        rpcCounter = new Semaphore(MAX_CONCURRENT_RPCS, true);
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
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
