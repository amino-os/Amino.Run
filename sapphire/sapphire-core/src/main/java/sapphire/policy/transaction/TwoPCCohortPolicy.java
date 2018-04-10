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
        private ParticipantManagerProvider participantManagerProvider = TransactionContext::getParticipants;

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

        // setter for test purpose
        void setParticipantManagerProvider(ParticipantManagerProvider provider) {
            this.participantManagerProvider = provider;
        }
    }

    /**
     * DCAP distributed transaction default server policy
     */
    public static class TwoPCCohortServerPolicy extends DefaultServerPolicy {
    }

    /**
     * DCAP distributed transaction default group policy
     */
    public static class TwoPCCohortGroupPolicy extends DefaultGroupPolicy {
    }
}
