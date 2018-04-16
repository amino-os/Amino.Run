package sapphire.policy.transaction;

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
    private TwoPCCohortPolicy.TwoPCCohortServerPolicy master;
    private SandboxProvider sandboxProvider;

    public NonconcurrentTransactionValidator(TwoPCCohortPolicy.TwoPCCohortServerPolicy master, SandboxProvider sandboxProvider) {
        this.master = master;
        this.sandboxProvider = sandboxProvider;
    }

    @Override
    public boolean promises(UUID transactionId) throws Exception {
        if (!this.promised.isEmpty()) {
            return false;
        }

        AppObjectShimServerPolicy sandbox = (AppObjectShimServerPolicy) this.sandboxProvider.getSandbox(this.master, transactionId);
        if (sandbox.getOriginMaster() != this.master.sapphire_getAppObject()) {
            return false;
        }

        this.promised.add(transactionId);
        return true;
    }

    @Override
    public void onCommit(UUID transactionId) {
        this.promised.remove(transactionId);
    }

    @Override
    public void onAbort(UUID transactionId) {
        this.promised.remove(transactionId);
    }
}
