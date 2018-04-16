package sapphire.policy.transaction;

import sapphire.policy.SapphirePolicyUpcalls.SapphireServerPolicyUpcalls;
import static sapphire.policy.transaction.TwoPCCohortPolicy.TwoPCCohortServerPolicy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * sandbox provider that manages sandbox containing the enclosed app object only
 */
public class AppObjectSandboxProvider implements SandboxProvider {
    private ConcurrentHashMap<UUID, AppObjectShimServerPolicy> sandboxes = new ConcurrentHashMap<UUID, AppObjectShimServerPolicy>();

    @Override
    public SapphireServerPolicyUpcalls getSandbox(TwoPCCohortServerPolicy origin, UUID transactionId) throws Exception {
        if (!this.sandboxes.containsKey(transactionId)) {
            AppObjectShimServerPolicy sandbox = AppObjectShimServerPolicy.cloneInShimServerPolicy(origin.sapphire_getAppObject());
            this.sandboxes.put(transactionId, sandbox);
        }

        return this.sandboxes.get(transactionId);
    }

    @Override
    public void removeSandbox(TwoPCCohortServerPolicy origin, UUID transactionId) {
        this.sandboxes.remove(transactionId);
    }
}
