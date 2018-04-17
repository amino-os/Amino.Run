package sapphire.policy.transaction;

import sapphire.policy.DefaultSapphirePolicy;

import java.util.ArrayList;
import java.util.UUID;

/**
 * DCAP distributed transaction default DM set
 */
public class TwoPCCohortPolicy extends DefaultSapphirePolicy {
    /**
     * DCAP distributed transaction default client policy
     */
    public static class TwoPCCohortClientPolicy extends DefaultClientPolicy implements TwoPCClient {
        public interface ParticipantManagerProvider {
            TwoPCParticipants Get();
        }

        private TwoPCParticipants participantManager;

        // the default participants provider
        private final ParticipantManagerProvider participantManagerProvider = new ParticipantManagerProvider() {
            @Override
            public TwoPCParticipants Get() {
                return TransactionContext.getParticipants();
            }
        };

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (super.hasTransaction()) {
                this.participantManager = this.participantManagerProvider.Get();

                this.participantManager.register(this);

                UUID txnId = this.getCurrentTransaction();
                TransactionWrapper txWrapper = new TransactionWrapper(txnId, method, params);
                return super.onRPC(TransactionWrapper.txWrapperTag, txWrapper.getRPCParams());
            }

            return super.onRPC(method, params);
        }
    }

    /**
     * DCAP distributed transaction default server policy
     */
    public static class TwoPCCohortServerPolicy extends DefaultServerPolicy {
        private final SandboxProvider sandboxProvider = new AppObjectSandboxProvider();
        private final TransactionManager transactionManager;

        public TwoPCCohortServerPolicy() {
            TransactionValidator validator = new NonconcurrentTransactionValidator(this.sapphire_getAppObject(), this.sandboxProvider);
            TLSTransactionManager transactionManager = new TLSTransactionManager();
            transactionManager.setValidator(validator);
            this.transactionManager = transactionManager;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            TransactionWrapper tx = new TransactionWrapper(method, params);
            UUID transactionId = tx.getTransaction();

            if (transactionId == null) {
                return super.onRPC(tx.getInnerRPCMethod(), tx.getInnerRPCParams());
            } else {
                return onTransactionRPC(tx);
            }
        }

        private Object onTransactionRPC(TransactionWrapper tx) throws Exception {
            UUID transactionId = tx.getTransaction();
            String rpcMethod = tx.getInnerRPCMethod();
            ArrayList<Object> rpcParams = tx.getInnerRPCParams();

            if (TwoPCPrimitive.isVoteRequest(rpcMethod)) {
                return this.transactionManager.vote(transactionId);
            }
            if (TwoPCPrimitive.isCommit(rpcMethod)) {
                this.transactionManager.commit(transactionId);
                SapphireServerPolicyUpcalls sandbox = this.sandboxProvider.getSandbox(this, transactionId);
                this.makeUpdateDurable(sandbox);
                this.sandboxProvider.removeSandbox(this, transactionId);
                return null;
            }
            if (TwoPCPrimitive.isAbort(rpcMethod)) {
                this.transactionManager.abort(transactionId);
                this.sandboxProvider.removeSandbox(this, transactionId);
                return null;
            } else {
                this.transactionManager.join(transactionId);
                SapphireServerPolicyUpcalls sandbox = this.sandboxProvider.getSandbox(this, transactionId);
                Object result = sandbox.onRPC(rpcMethod, tx.getInnerRPCParams());
                this.transactionManager.leave(transactionId);
                return result;
            }
        }

        /**
         * replaces appObject etc with that of the sandbox to observe durability
         * @param sandbox the isolated object that holds the updated content
         */
        private void makeUpdateDurable(SapphireServerPolicyUpcalls sandbox) {
            AppObjectShimServerPolicy shimServerPolicy = (AppObjectShimServerPolicy)sandbox;
            this.appObject = shimServerPolicy.getAppObject();
        }
    }

    /**
     * DCAP distributed transaction default group policy
     */
    public static class TwoPCCohortGroupPolicy extends DefaultGroupPolicy {
    }
}
