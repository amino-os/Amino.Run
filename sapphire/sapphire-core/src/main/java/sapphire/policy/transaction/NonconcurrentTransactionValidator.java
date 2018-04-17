package sapphire.policy.transaction;

import sapphire.common.AppObject;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * simplistic one of the transaction validator - only allowing one transaction, any
 * commit invalidates the unfinished ones.
 */
public class NonconcurrentTransactionValidator implements TransactionValidator{
    private Set<UUID> promised = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private AppObject master;
    private SandboxProvider sandboxProvider;

    public NonconcurrentTransactionValidator(AppObject master, SandboxProvider sandboxProvider) {
        this.master = master;
        this.sandboxProvider = sandboxProvider;
    }

    @Override
    public boolean promises(UUID transactionId) throws Exception {
        AppObjectShimServerPolicy sandbox = (AppObjectShimServerPolicy) this.sandboxProvider.getSandbox(null, transactionId);

        synchronized (this) {
            if (sandbox.getOriginMaster() != this.master) {
                return false;
            }

            if (!this.promised.isEmpty()) {
                return false;
            }

            this.promised.add(transactionId);
            return true;
        }
    }

    @Override
    public void onCommit(UUID transactionId) {
        AppObjectShimServerPolicy sandbox = null;
        try {
            sandbox = (AppObjectShimServerPolicy) this.sandboxProvider.getSandbox(null, transactionId);
        } catch (Exception e) {
            // todo: handle exception properly
            // note: getSandbox should not fail at this moment - todo: refactor in order to provide such guarantee
            // note: onCommit itself should not throw exception
        }

        synchronized (this) {
            this.master = sandbox.getAppObject();
            this.promised.remove(transactionId);
        }
    }

    @Override
    synchronized public void onAbort(UUID transactionId) {
        this.promised.remove(transactionId);
    }
}
