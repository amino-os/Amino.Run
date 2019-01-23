package amino.run.policy.transaction;

import static amino.run.policy.Library.ServerPolicyLibrary;
import static amino.run.policy.Upcalls.ServerUpcalls;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** sandbox provider that manages sandbox containing the enclosed app object only */
public class AppObjectSandboxProvider implements SandboxProvider, Serializable {
    private ConcurrentHashMap<UUID, AppObjectShimServerPolicy> sandboxes =
            new ConcurrentHashMap<UUID, AppObjectShimServerPolicy>();

    @Override
    public ServerUpcalls getSandbox(ServerPolicyLibrary origin, UUID transactionId)
            throws Exception {
        if (!this.sandboxes.containsKey(transactionId)) {
            AppObjectShimServerPolicy sandbox =
                    AppObjectShimServerPolicy.cloneInShimServerPolicy(
                            origin.sapphire_getAppObject());
            this.sandboxes.put(transactionId, sandbox);
        }

        return this.sandboxes.get(transactionId);
    }

    @Override
    public ServerUpcalls getSandbox(UUID transactionId) {
        return this.sandboxes.get(transactionId);
    }

    @Override
    public void removeSandbox(UUID transactionId) {
        this.sandboxes.remove(transactionId);
    }
}
