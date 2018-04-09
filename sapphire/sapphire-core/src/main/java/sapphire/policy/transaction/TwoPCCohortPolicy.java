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
    public static class TwoPCCohortClientPolicy extends DefaultClientPolicy implements I2PCClient {
        public interface IParticipantManagerProvider {
            I2PCParticipants Get();
        }

        private I2PCParticipants participantManager;

        private IParticipantManagerProvider participantManagerProvider = () -> {return TransactionContext.getParticipants();};

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (super.hasTransaction()) {
                this.participantManager = this.participantManagerProvider.Get();

                this.registerInTansaction();

                UUID txnId = this.getCurrentTransaction();
                TransactionWrapper txWrapper = new TransactionWrapper(txnId, method, params);
                return super.onRPC(TransactionWrapper.txWrapperTag, txWrapper.getRpcParams());
            }

            return super.onRPC(method, params);
        }

        // setter for test purpose
        void setParticipantManagerProvider(IParticipantManagerProvider provider) {
            this.participantManagerProvider = provider;
        }

        private void registerInTansaction() {
            this.participantManager.register(this);
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
