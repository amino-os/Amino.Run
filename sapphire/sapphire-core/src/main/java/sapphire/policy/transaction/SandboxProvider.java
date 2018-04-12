package sapphire.policy.transaction;

import java.util.UUID;

import static sapphire.policy.SapphirePolicyUpcalls.SapphireServerPolicyUpcalls;
import static sapphire.policy.transaction.TwoPCCohortPolicy.TwoPCCohortServerPolicy;

/**
 * type to provide sandbox
 */
public interface SandboxProvider {
    /**
     * gets the sandbox of the origin thing associated with specified transaction
     * @param origin the origin of thing
     * @param transactionId id of the transaction
     * @return sandbox associated with the transaction
     */
    SapphireServerPolicyUpcalls getSandbox(TwoPCCohortServerPolicy origin, UUID transactionId);

    /**
     * removes the sandbox of the origin thing associated with the specified transaction
     * @param origin the origin thing
     * @param transactionId id of the transaction
     */
    void removeSandbox(TwoPCCohortServerPolicy origin, UUID transactionId);
}
