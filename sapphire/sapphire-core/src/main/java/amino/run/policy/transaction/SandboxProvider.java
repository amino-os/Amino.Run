package amino.run.policy.transaction;

import amino.run.policy.Library;
import amino.run.policy.SapphirePolicyUpcalls;
import java.util.UUID;

/** type to provide sandbox */
public interface SandboxProvider {
    /**
     * gets the sandbox of the origin thing associated with specified transaction; if not exists
     * previously, creates one.
     *
     * @param origin the origin of thing
     * @param transactionId id of the transaction
     * @return sandbox associated with the transaction
     */
    SapphirePolicyUpcalls.SapphireServerPolicyUpcalls getSandbox(
            Library.SapphireServerPolicyLibrary origin, UUID transactionId) throws Exception;

    /**
     * gets the sandbox assiated with the specified transaction
     *
     * @param transactionId id of the transaction
     * @return sandbox assiciated with the transaction
     */
    SapphirePolicyUpcalls.SapphireServerPolicyUpcalls getSandbox(UUID transactionId);

    /**
     * removes the sandbox associated with the specified transaction
     *
     * @param transactionId id of the transaction
     */
    void removeSandbox(UUID transactionId);
}
