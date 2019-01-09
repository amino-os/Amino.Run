package amino.run.policy.transaction;

import static amino.run.policy.SapphirePolicyLibrary.SapphireServerPolicyLibrary;
import static amino.run.policy.SapphirePolicyUpcalls.SapphireServerPolicyUpcalls;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** sandbox provider that manages sandbox containing the enclosed app object only */
public class AppObjectSandboxProvider implements SandboxProvider, Serializable {
    private ConcurrentHashMap<UUID, AppObjectShimServerPolicy> sandboxes =
            new ConcurrentHashMap<UUID, AppObjectShimServerPolicy>();

    @Override
    public SapphireServerPolicyUpcalls getSandbox(
            SapphireServerPolicyLibrary origin, UUID transactionId) throws Exception {
        if (!this.sandboxes.containsKey(transactionId)) {
            AppObjectShimServerPolicy sandbox =
                    AppObjectShimServerPolicy.cloneInShimServerPolicy(
                            origin.sapphire_getAppObject());
            this.sandboxes.put(transactionId, sandbox);
        }

        return this.sandboxes.get(transactionId);
    }

    @Override
    public SapphireServerPolicyUpcalls getSandbox(UUID transactionId) {
        return this.sandboxes.get(transactionId);
    }

    @Override
    public void removeSandbox(UUID transactionId) {
        this.sandboxes.remove(transactionId);
    }
}
