package sapphire.policy.transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import sapphire.policy.DefaultSapphirePolicy;

/** DCAP distributed transaction default DM set */
public class TwoPCCohortPolicy extends DefaultSapphirePolicy {
    /** DCAP distributed transaction default client policy */
    public static class TwoPCCohortClientPolicy extends DefaultClientPolicy
            implements TwoPCClient, Serializable {
        public interface ParticipantManagerProvider {
            TwoPCParticipants Get();
        }

        private TwoPCParticipants participantManager;

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if ((!method.equals(TransactionWrapper.txWrapperTag)) && super.hasTransaction()) {
                TransactionContext.getParticipants().register(this);

                UUID txnId = this.getCurrentTransaction();
                TransactionWrapper txWrapper = new TransactionWrapper(txnId, method, params);
                return super.onRPC(TransactionWrapper.txWrapperTag, txWrapper.getRPCParams());
            }

            return super.onRPC(method, params);
        }
    }

    /** DCAP distributed transaction default server policy */
    public static class TwoPCCohortServerPolicy extends DefaultServerPolicy {
        protected final SandboxProvider sandboxProvider = new AppObjectSandboxProvider();
        private TransactionManager transactionManager;

        private static Logger logger = Logger.getLogger(TwoPCCohortServerPolicy.class.getName());

        protected TwoPCCohortServerPolicy(TransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        protected void setTransactionManager(TransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        public TwoPCCohortServerPolicy() {
            TransactionValidator validator =
                    new NonconcurrentTransactionValidator(
                            this.sapphire_getAppObject(), this.sandboxProvider);
            this.transactionManager = new TLSTransactionManager();
            ((TLSTransactionManager) this.transactionManager).setValidator(validator);
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
                SapphireServerPolicyUpcalls sandbox =
                        this.sandboxProvider.getSandbox(transactionId);
                this.makeUpdateDurable(sandbox);
                this.sandboxProvider.removeSandbox(transactionId);
                return null;
            }
            if (TwoPCPrimitive.isAbort(rpcMethod)) {
                this.transactionManager.abort(transactionId);
                this.sandboxProvider.removeSandbox(transactionId);
                return null;
            } else {
                SapphireServerPolicyUpcalls sandbox =
                        this.sandboxProvider.getSandbox(this, transactionId);
                this.transactionManager.join(transactionId);
                try {
                    return sandbox.onRPC(rpcMethod, tx.getInnerRPCParams());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "onRPC failed: ", e);
                    this.transactionManager.abort(transactionId);
                    return null;
                } finally {
                    this.transactionManager.leave(transactionId);
                }
            }
        }

        /**
         * replaces appObject etc with that of the sandbox to observe durability
         *
         * @param sandbox the isolated object that holds the updated content
         */
        private void makeUpdateDurable(SapphireServerPolicyUpcalls sandbox) {
            AppObjectShimServerPolicy shimServerPolicy = (AppObjectShimServerPolicy) sandbox;
            this.appObject = shimServerPolicy.getAppObject();
        }
    }

    /** DCAP distributed transaction default group policy */
    public static class TwoPCCohortGroupPolicy extends DefaultGroupPolicy {}
}
