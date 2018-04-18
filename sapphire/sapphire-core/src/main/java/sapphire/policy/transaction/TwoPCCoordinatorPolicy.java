package sapphire.policy.transaction;

import sapphire.policy.DefaultSapphirePolicy;

import java.util.ArrayList;
import java.util.UUID;

/**
 * DCAP distributed transaction coordinator default DM set
 */
public class TwoPCCoordinatorPolicy extends DefaultSapphirePolicy {
    /**
     * DCAP distributed transaction coordinator client policy
     */
    public static class TwoPCCoordinatorClientPolicy extends DefaultClientPolicy {}

    /**
     * DCAP distributed transaction coordinator server policy
     */
    public static class TwoPCCoordinatorServerPolicy extends DefaultServerPolicy {
        private TwoPCCoordinator coordinator;
        private final SandboxProvider sandboxProvider = new AppObjectSandboxProvider();

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            this.coordinator.beginTransaction();
            UUID transactionId = this.coordinator.getTransactionId();

            SapphireServerPolicyUpcalls sandbox = this.sandboxProvider.getSandbox(this, transactionId);

            Object rpcResult;
            try {
                rpcResult = sandbox.onRPC(method, params);
            }catch (Exception e) {
                this.coordinator.abort(transactionId);
                throw new TransactionAbortException("execution had error.", e);
            }

            TransactionManager.Vote vote = this.coordinator.vote(transactionId);

            if (TransactionManager.Vote.YES.equals(vote)) {
                this.coordinator.commit(transactionId);
                this.makeUpdateDurable(sandbox);
                return rpcResult;
            } else {
                this.coordinator.abort(transactionId);
                // todo: to gather the detail of invalidation
                throw new TransactionAbortException("transaction was in invalid state.", null);
            }
        }

        private void makeUpdateDurable(SapphireServerPolicyUpcalls sandbox) {
            AppObjectShimServerPolicy shimServerPolicy = (AppObjectShimServerPolicy)sandbox;
            this.appObject = shimServerPolicy.getAppObject();
        }
    }

    /**
     * DCAP distributed transaction coordinator group policy
     */
    public static class TwoPCCoordinatorGroupPolicy extends DefaultGroupPolicy {}
}