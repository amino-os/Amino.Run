package sapphire.kernel.server;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import sapphire.common.ObjectHandler;
import sapphire.kernel.common.KernelObjectMigratingException;

/** 
 * A single Sapphire kernel object that can receive RPCs.
 * These are stored in the Sapphire kernel server. 
 * @author iyzhang
 *
 */

public class KernelObject extends ObjectHandler {

	private static final int MAX_CONCURRENT_RPCS = 100;
	private Boolean coalesced;
	private Semaphore rpcCounter;
	
	public KernelObject(Object obj) {
		super(obj);
		coalesced = false;
		rpcCounter = new Semaphore(MAX_CONCURRENT_RPCS, true);
	}
	
	public Object invoke(String method, ArrayList<Object> params) throws Exception {
		Object ret;
		
		if (coalesced) {
			throw new KernelObjectMigratingException();
		}
		
		rpcCounter.acquire();
		ret = super.invoke(method, params);
		rpcCounter.release();
		
		return ret;
	}
	
	public void coalesce() {
		coalesced = true;
		while (rpcCounter.availablePermits() < MAX_CONCURRENT_RPCS - 1) {
			try {
				Thread.sleep(10);
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