package sapphire.policy.transaction;

import sapphire.policy.SapphirePolicyUpcalls.SapphireServerPolicyUpcalls;
import sapphire.policy.serializability.TransactionAlreadyStartedException;

import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * transaction manager type for external resource.
 * It uses an internal transaction manager to book-keep the in-memory SO transaction status,
 * also consult the business object through its well-defined interface for the external resource transaction condition.
 */
public class ExtResourceTransactionManager implements TransactionManager, Serializable {
    private SandboxProvider sandboxProvider;
    private TransactionManager internalTransactionManager;
    private static Logger logger = Logger.getLogger(ExtResourceTransactionManager.class.getName());

    public ExtResourceTransactionManager(SandboxProvider sandboxProvider, TransactionManager internalTransactionManager) {
        this.sandboxProvider = sandboxProvider;
        this.internalTransactionManager = internalTransactionManager;
    }

    private TransactionManager getBusinessObject(UUID transactionId) {
        SapphireServerPolicyUpcalls sandbox = this.sandboxProvider.getSandbox(transactionId);
        AppObjectShimServerPolicy appObjectShimServerPolicy = (AppObjectShimServerPolicy)sandbox;
        return (TransactionManager)appObjectShimServerPolicy.getAppObject().getObject();
    }

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {
        this.internalTransactionManager.join(transactionId);
        this.getBusinessObject(transactionId).join(transactionId);
    }

    @Override
    public void leave(UUID transactionId) {
        this.internalTransactionManager.leave(transactionId);
        this.getBusinessObject(transactionId).leave(transactionId);
    }

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        Vote vote = this.internalTransactionManager.vote(transactionId);

        if (vote.equals(Vote.YES)) {
            Vote voteExtResource = this.getBusinessObject(transactionId).vote(transactionId);

            if (!Vote.YES.equals(voteExtResource)) {
                logger.log(Level.WARNING, "inconsistent resource state; transaction aborted.");
                this.internalTransactionManager.abort(transactionId);
                vote = Vote.NO;
            }
        }

        return vote;
    }

    @Override
    public void commit(UUID transactionId) {
        this.getBusinessObject(transactionId).commit(transactionId);
        this.internalTransactionManager.commit(transactionId);
    }

    @Override
    public void abort(UUID transactionId) {
        this.getBusinessObject(transactionId).abort(transactionId);
        this.internalTransactionManager.abort(transactionId);
    }
}
