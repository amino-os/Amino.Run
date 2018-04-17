package sapphire.policy.transaction;

import sapphire.policy.SapphirePolicyLibrary.SapphireServerPolicyLibrary;

import java.util.UUID;

import static sapphire.policy.SapphirePolicyUpcalls.SapphireServerPolicyUpcalls;

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
    SapphireServerPolicyUpcalls getSandbox(SapphireServerPolicyLibrary origin, UUID transactionId) throws Exception;

    /**
     * removes the sandbox of the origin thing associated with the specified transaction
     * @param origin the origin thing
     * @param transactionId id of the transaction
     */
    void removeSandbox(SapphireServerPolicyLibrary origin, UUID transactionId);
}
